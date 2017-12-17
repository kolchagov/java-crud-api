/*
 *  Copyright (c) 2017. I.Kolchagov, All rights reserved.
 *  Contact: I.Kolchagov (kolchagov (at) gmail.com)
 *
 *  The contents of this file is licensed under the terms of LGPLv3 license.
 *  You may read the the included file 'lgpl-3.0.txt'
 *  or https://www.gnu.org/licenses/lgpl-3.0.txt
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  The project uses 'fluentsql' internally, licensed under Apache Public License v2.0.
 *  https://github.com/ivanceras/fluentsql/blob/master/LICENSE.txt
 *
 */

package eu.hadeco.crudapi;/*
 * Copyright (C) <2014> <Michele Bonazza>
 * 
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.util.*;

/**
 * A test runner that runs tests according to their position in the source file
 * of the test class.
 *
 * @author Michele Bonazza
 */
public class OrderedTestRunner extends BlockJUnit4ClassRunner {

    /**
     * Creates a new runner
     *
     * @param clazz the class being tested
     * @throws InitializationError if something goes wrong
     */
    public OrderedTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.junit.runners.BlockJUnit4ClassRunner#computeTestMethods()
     */
    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        // get all methods to be tested
        List<FrameworkMethod> toSort = super.computeTestMethods();

        if (toSort.isEmpty())
            return toSort;

        // a map containing <line_number, method>
        final Map<Integer, FrameworkMethod> testMethods = new TreeMap<>();

        // check that all methods here are declared in the same class, we don't
        // deal with test methods from superclasses that haven't been overridden
        Class<?> clazz = getDeclaringClass(toSort);
        if (clazz == null) {
            // fail explicitly
            System.err
                    .println("OrderedTestRunner can only run test classes that"
                            + " don't have test methods inherited from superclasses");
            return Collections.emptyList();
        }

        // use Javassist to figure out line numbers for methods
        ClassPool pool = ClassPool.getDefault();
        try {
            CtClass cc = pool.get(clazz.getName());
            // all methods in toSort are declared in the same class, we checked
            for (FrameworkMethod m : toSort) {
                String methodName = m.getName();
                CtMethod method = cc.getDeclaredMethod(methodName);
                testMethods.put(method.getMethodInfo().getLineNumber(0), m);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(testMethods.values());
    }

    private Class<?> getDeclaringClass(List<FrameworkMethod> methods) {
        // methods can't be empty, it's been checked
        Class<?> clazz = methods.get(0).getMethod().getDeclaringClass();

        for (int i = 1; i < methods.size(); i++) {
            if (!methods.get(i).getMethod().getDeclaringClass().equals(clazz)) {
                // they must be all in the same class
                return null;
            }
        }

        return clazz;
    }
}