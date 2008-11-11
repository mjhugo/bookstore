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
 * Support class for writing unit tests in Grails. It mainly provides
 * access to various mocking options, while making sure that the meta-
 * class magic does not leak outside of a single test.
 */
class GrailsUnitTestCase extends GroovyTestCase {
    def savedMetaClasses

    void setUp() {
        super.setUp()
        savedMetaClasses = [:]
    }

    void tearDown() {
        super.tearDown()

        // Restore all the saved meta classes.
        savedMetaClasses.each { clazz, metaClass ->
            GroovySystem.metaClassRegistry.setMetaClass(clazz, metaClass)
        }
    }

    /**
     * Use this method when you plan to perform some meta-programming
     * on a class. It ensures that any modifications you make will be
     * cleared at the end of the test.
     * @param clazz The class to register.
     */
    protected void registerMetaClass(Class clazz) {
        // If the class has already been registered, then there's
        // nothing to do.
        if (savedMetaClasses.containsKey(clazz)) return

        // Save the class's current meta class.
        savedMetaClasses[clazz] = clazz.metaClass

        // Create a new EMC for the class and attach it.
        def emc = new ExpandoMetaClass(clazz, true, true)
        emc.initialize()
        GroovySystem.metaClassRegistry.setMetaClass(clazz, emc)
    }

    /**
     * Creates a new Grails mock for the given class. Use it as you
     * would use MockFor and StubFor.
     * @param clazz The class to mock.
     * @param loose If <code>true</code>, the method returns a loose-
     * expectation mock, otherwise it returns a strict one. The default
     * is a strict mock.
     */
    protected GrailsMock mockFor(Class clazz, boolean loose = false) {
        registerMetaClass(clazz)
        return new GrailsMock(clazz, loose)
    }

    /**
     * Mocks a domain class, providing working implementations of the
     * standard dynamic methods. A list of domain instances can be
     * provided as a source of data for the methods, in particular
     * the queries.
     * @param domainClass The class to mock.
     * @param instances An optional list of domain instances to use
     * as the data backing the dynamic methods.
     */
    protected void mockDomain(Class domainClass, List instances = []) {
        registerMetaClass(domainClass)
        MockUtils.mockDomain(domainClass, instances)
    }

    /**
     * Mocks a controller class, providing implementations of standard methods
     * like render and redirect
     */
    protected void mockController(Class controllerClass) {
        registerMetaClass(controllerClass)
        MockUtils.mockController(controllerClass)
    }

    /**
     * Mocks a tag library, providing the common web properties as
     * well as "out", "throwTagError()", and an implementation of
     * the "render" tag.
     */
    protected void mockTagLib(Class tagLibClass) {
        registerMetaClass(tagLibClass)
        MockUtils.mockTagLib(tagLibClass)
    }

    /**
     * Provides a mock implementation of the "log" property for the
     * given class. By default, debug and trace levels are ignored
     * but you can enable printing of debug statements via the <code>
     * enableDebug</code> argument.
     * @param clazz The class to add the log method to.
     * @param enableDebug An optional flag to switch on printing of
     * debug statements.
     */
    protected void mockLogging(Class clazz, boolean enableDebug = false) {
        registerMetaClass(clazz)

        MockUtils.mockLogging(clazz, enableDebug)
    }
}
