/* Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.test

/**
 * Support class for writing unit tests for tag libraries. Its main job
 * is to mock the various properties and methods that Grails injects
 * into taglibs. By default it determines what tag library to mock
 * based on the name of the test, but this can be overridden by one
 * of the constructors.
 */
class TagLibUnitTestCase extends MvcUnitTestCase {
    protected tagLib

    /**
     * Creates a new test case for the controller that is in the same
     * package as the test case and has the same prefix before "Controller"
     * in its name. For example, if the class name of the test were
     * <code>org.example.TestControllerTests</code>, this constructor
     * would mock <code>org.example.TestController</code>.
     */
    TagLibUnitTestCase() {
        super("TagLib")
    }

    /**
     * Creates a new test case for the given controller class.
     */
    TagLibUnitTestCase(Class tagLibClass) {
        super(tagLibClass)
    }

    void setUp() {
        super.setUp()
        mockTagLib(this.testClass)
        this.tagLib = newInstance()
    }

    Class getTagLibClass() {
        return this.testClass
    }
}
