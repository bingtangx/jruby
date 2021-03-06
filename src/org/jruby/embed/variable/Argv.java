/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2011 Yoko Harada <yokolet@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyObject;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author yoko
 */
public class Argv extends AbstractVariable {
    private static String pattern = "ARGV";

    /**
     * Returns an instance of this class. This factory method is used when an ARGV
     * is put in {@link BiVariableMap}.
     *
     * @param runtime
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of Constant
     */
    public static BiVariable getInstance(RubyObject receiver, String name, Object... javaObject) {
        if (name.matches(pattern)) {
            return new Argv(receiver, name, javaObject);
        }
        return null;
    }
    
    private Argv(RubyObject receiver, String name, Object... javaObjects) {
        super(receiver, name, false);
        assert javaObjects != null;
        javaObject = javaObjects[0];
        if (javaObject == null) {
            javaType = null;
        } else if (javaObjects.length > 1) {
            javaType = (Class) javaObjects[1];
        } else {
            javaType = javaObject.getClass();
        }
    }
    
    private void updateArgvByJavaObject() {
        RubyArray ary = RubyArray.newArray(receiver.getRuntime());
        if (javaObject instanceof Collection) {
            ary.addAll((Collection)javaObject);
        } else if (javaObject instanceof String[]) {
            for (String s : (String[])javaObject) {
                ary.add(s);
            }
        }
        irubyObject = ary;
    }

    /**
     * A constructor used when ARGV is retrieved from Ruby.
     *
     * @param receiver a receiver object that this variable/constant is originally in. When
     *        the variable/constant is originated from Ruby, receiver may not be null.
     * @param name the constant name
     * @param irubyObject Ruby constant object
     */
    Argv(IRubyObject receiver, String name, IRubyObject irubyObject) {
        super(receiver, name, true, irubyObject);
    }

    /**
     * Returns enum type of this variable defined in {@link BiVariable}.
     *
     * @return this enum type, BiVariable.Type.InstanceVariable.
     */
    public Type getType() {
        return Type.Argv;
    }
    
    /**
     * Returns true if the given name is ARGV. Unless returns false.
     *
     * @param name is a name to be checked.
     * @return true if the given name is ARGV.
     */
    public static boolean isValidName(Object name) {
        return isValidName(pattern, name);
    }

    /**
     * Injects ARGV values to a parsed Ruby script. This method is
     * invoked during EvalUnit#run() is executed.
     *
     * @param runtime is environment where a variable injection occurs
     * @param receiver is the instance that will have variable injection.
     */
    public void inject() {
        updateArgvByJavaObject();
        RubyModule rubyModule = getRubyClass(receiver.getRuntime());
        if (rubyModule == null) rubyModule = receiver.getRuntime().getCurrentContext().getRubyClass();
        if (rubyModule == null) return;

        rubyModule.storeConstant(name, irubyObject);
        receiver.getRuntime().getConstantInvalidator().invalidate();
        fromRuby = true;
    }

    /**
     * Removes this object from {@link BiVariableMap}. Also, initialize
     * this variable in top self.
     *
     */
    public void remove() {
        javaObject = new ArrayList();
        inject();
    }
    
   /**
     * Retrieves ARGV from Ruby after the evaluation or method invocation.
     *
     * @param runtime Ruby runtime
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved constants.
     */
    public static void retrieve(RubyObject receiver, BiVariableMap vars) {
        if (vars.isLazy()) return;
        updateARGV(receiver, vars);
    }
    
    private static void updateARGV(IRubyObject receiver, BiVariableMap vars) {
        String name = "ARGV".intern();
        IRubyObject argv = receiver.getRuntime().getTopSelf().getMetaClass().getConstant(name);
        if (argv == null || (argv instanceof RubyNil)) return;
        BiVariable var;  // This var is for ARGV.
        // ARGV constant should be only one
        if (vars.containsKey((Object)name)) {
            var = vars.getVariable((RubyObject)receiver.getRuntime().getTopSelf(), name);
            var.setRubyObject(argv);
        } else {
            var = new Constant(receiver.getRuntime().getTopSelf(), name, argv);
            ((Constant) var).markInitialized();
            vars.update(name, var);
        }
    }
    
    /**
     * Retrieves ARGV by key from Ruby runtime after the evaluation.
     * This method is used when eager retrieval is off.
     *
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved instance variables.
     * @param key instace varible name
     */
    public static void retrieveByKey(RubyObject receiver, BiVariableMap vars, String key) {
        assert key.equals("ARGV");
        updateARGV(receiver, vars);
    }
    
    @Override
    public Object getJavaObject() {
        if (irubyObject == null || !fromRuby) {
            return javaObject;
        }
        RubyArray ary = (RubyArray)irubyObject;
        if (javaType == null) { // firstly retrieved from Ruby
            javaObject = new ArrayList<String>();
            ((ArrayList)javaObject).addAll(ary);
            return javaObject;
        } else if (javaType == String[].class) {
            javaObject = new String[ary.size()];
            for (int i=0; i<ary.size(); i++) {
                ((String[])javaObject)[i] = (String) ary.get(i);
            }
            return javaObject;
        } else if (javaObject instanceof List) {
            try {
                ((List)javaObject).clear();
                ((List)javaObject).addAll(ary);
            } catch (UnsupportedOperationException e) {
                // no op. no way to update.
            }
            return javaObject;
        }
        return null;
    }
}
