package grails.test

import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.springframework.mock.web.MockHttpSession

/**
 * Common test case support class for controllers, tag libraries, and
 * anything else that has access to the standard web properties such
 * as "request", "response", and "session".
 */
class MvcUnitTestCase extends GrailsUnitTestCase {
    private Class testClass

    protected GrailsMockHttpServletRequest mockRequest
    protected GrailsMockHttpServletResponse mockResponse
    protected MockHttpSession mockSession

    protected Map redirectArgs
    protected Map renderArgs
    protected Map mockParams
    protected Map mockFlash

    /**
     * Creates a new test case for the class whose name and package
     * matches this test's class up to and including the given suffix.
     * In other words, if this test is <code>org.example.MyControllerTests</code>
     * then the class under test is <code>org.example.MyController</code>.
     * This example assumes that the suffix is "Controller".
     */
    MvcUnitTestCase(String suffix) {
        def m = getClass().name =~ /^([\w\.]*?[A-Z]\w*?${suffix})\w+/
        if (m) {
            this.testClass = Thread.currentThread().contextClassLoader.loadClass(m[0][1])
        }
        else {
            throw new RuntimeException("Cannot find matching class for this test.")
        }
    }

    /**
     * Creates a new test case for the given class.
     */
    MvcUnitTestCase(Class clazz) {
        this.testClass = clazz
    }

    void setUp() {
        super.setUp()
    }

    Class getTestClass() {
        return this.testClass
    }

    protected void reset() {
        mockRequest.clearAttributes()
        mockRequest.removeAllParameters()
        mockResponse.committed = false
        mockSession.clearAttributes()
        mockSession.setNew(true)

        redirectArgs.clear()
        renderArgs.clear()
        mockParams.clear()
        mockFlash.clear()
    }

    protected newInstance() {
        def instance = this.testClass.newInstance()

        redirectArgs = instance.redirectArgs
        renderArgs = instance.renderArgs
        mockRequest = instance.request
        mockResponse = instance.response
        mockSession = instance.session

        mockParams = instance.params
        mockFlash = instance.flash
    }
}
