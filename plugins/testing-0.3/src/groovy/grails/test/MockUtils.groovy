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

import groovy.xml.StreamingMarkupBuilder
import java.beans.Introspector
import java.beans.PropertyDescriptor
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.plugins.testing.GrailsMockErrors
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder
import org.codehaus.groovy.grails.web.converters.Converter
import org.springframework.mock.web.MockHttpSession
import org.springframework.validation.Errors
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.codehaus.groovy.grails.web.converters.ConverterUtil

/**
 * This is a utility/helper class for mocking various types of Grails
 * artifacts and is one of the foundations of the Grails unit testing
 * framework.
 *
 * @author Peter Ledbrook
 */
class MockUtils {
    static final COMPARATORS = Collections.unmodifiableList([
            "IsNull",
            "IsNotNull",
            "LessThan",
            "LessThanEquals",
            "GreaterThan",
            "GreaterThanEquals",
            "NotEqual",
            "Like",
            "Ilike",
            "Between" ])
    static final COMPARATORS_RE = COMPARATORS.join("|")
    static final DYNAMIC_FINDER_RE = /(\w+?)(${COMPARATORS_RE})?((And|Or)(\w+?)(${COMPARATORS_RE})?)?/

    static final errorsObjects = new ThreadLocalMap()

    /**
     * Enhances a class that has the method signatures setAttribute/getAttribute (such as the Request object) to allow property style access
     * @param clazz The class or interface to mock
     */
    static void mockAttributeAccess(Class clazz) {
        clazz.metaClass.getProperty = { String name ->
            if(delegate.metaClass.hasProperty(delegate,name)) {
                return delegate.metaClass.getMetaProperty(name).getProperty(delegate)
            }
            else {
                return delegate.getAttribute(name)
            }
        }
        clazz.metaClass.setProperty = { String name, val ->
            if(delegate.metaClass.hasProperty(delegate,name)) {
                delegate.metaClass.getMetaProperty(name).setProperty(delegate, val)
            }
            else {
                delegate.setAttribute(name,val)
            }
        }
    }
    
    /**
     * Call this to mock the given controller class. It adds mock versions of
     * the various methods available on controllers that are normally provided
     * by Grails such as render and redirect.
     * @param clazz The class of the controller to mock
     */
    static void mockController(Class clazz) {
        mockLogging(clazz)
        addCommonWebProperties(clazz)

        // Set up the argument maps and method implementations for the
        // render() and redirect() methods.
        def redArgs = [:]
        def renArgs = [:]
        def template = [:]
        def modelAndView = [:]
        clazz.metaClass.getRedirectArgs ={-> redArgs}
        clazz.metaClass.getRenderArgs ={-> renArgs}
        clazz.metaClass.redirect = {Map map -> redirectArgs.putAll(map)}
        clazz.metaClass.render = {String text -> delegate.response.writer << text}
        clazz.metaClass.render = {Converter arg -> delegate.response.writer << arg.toString()}

        clazz.metaClass.withFormat = { Closure callable ->
            def formatInterceptor = new org.codehaus.groovy.grails.plugins.web.mimes.FormatInterceptor()
            def originalDelegate = delegate
            try {
                callable.delegate = formatInterceptor
                callable.resolveStrategy = Closure.DELEGATE_ONLY
                callable.call()
            }
            finally {
                callable.delegate = originalDelegate
                callable.resolveStrategy = Closure.OWNER_FIRST
            }
            def formats = formatInterceptor.formatOptions
            def response = null
            if(request.format && formats.containsKey(request.format)) {
                response = formats[request.format]
            }
            else {
                response = formats[formats.firstKey()]
            }
            if(response instanceof Closure) {
                return response.call()
            }
            else if(response instanceof Map) {
                return response
            }
        }

        clazz.metaClass.render = {Map map ->
            renderArgs.putAll(map)
            if (map["template"] != null) {
                assert map["view"] == null : "'view' cannot be used with 'template' in render"
                assert map["text"] == null : "'text' cannot be used with 'template' in render"

                template["name"] = map["template"]

                if (map["model"] != null) {
                    assert map["collection"] == null : "'collection' cannot be used with 'model' in render"
                    assert map["bean"] == null : "'bean' cannot be used with 'model' in render"

                    template["model"] = map["model"]
                }
                else if (map["collection"] != null) {
                    assert map["bean"] == null : "'bean' cannot be used with 'collection' in render"

                    template["collection"] = map["collection"]
                }
                else if (map["bean"] != null) {
                    template["bean"] = map["bean"]
                }
            }
            else if (map["view"] != null) {
                assert map["text"] == null : "'text' cannot be used with 'view' in render"

                modelAndView["view"] = map["view"]
                modelAndView["model"] = map["model"]
            }
            else if (map["text"] != null) {
                delegate.response.outputStream << map["text"]
            }
        }

        clazz.metaClass.render = {Map map, Closure c ->
            renderArgs.putAll(map)

            switch(map["contentType"]) {
            case null:
                break

            case "application/xml":
            case "text/xml":
                def b = new StreamingMarkupBuilder()
                if (map["encoding"]) b.encoding = map["encoding"]

                def writable = b.bind(c)
                delegate.response.outputStream << writable
                break

            default:
                println "Nothing"
                break
            }
        }

        clazz.metaClass.getTemplate = {-> template}
        clazz.metaClass.getModelAndView = {-> modelAndView}
    }

    /**
     * Call this to mock the given taglib class. It adds mock versions
     * of the various methods and properties available on tag libraries
     * that are normally provided by Grails. For example, it adds "flash",
     * "request", and the "render" tag. Other tags should be mocked
     * separately.
     * @param clazz The class of the tag library to mock.
     */
    static void mockTagLib(Class clazz) {
        mockLogging(clazz)
        addCommonWebProperties(clazz)

        // Set up the "out" property and the method for generating tag
        // errors.
        def mockOut = new StringWriter()
        clazz.metaClass.throwTagError = {String message -> throw new GrailsTagException(message) }
        clazz.metaClass.getOut = {-> mockOut }

        // Render tag (called as a method).
        def renArgs = [:]
        def template = [:]
        clazz.metaClass.getRenderArgs ={-> renArgs}
        clazz.metaClass.getTemplate = {-> template}

        clazz.metaClass.render = {Map map ->
            renderArgs.putAll(map)
            if (map["template"]) {
                assert !map["view"]  : "'view' cannot be used with 'template' in render"
                assert !map["text"] : "'text' cannot be used with 'template' in render"

                template["name"] = map["template"]

                if (map["model"]) {
                    assert !map["collection"] : "'collection' cannot be used with 'model' in render"
                    assert !map["bean"] : "'bean' cannot be used with 'model' in render"

                    template["model"] = map["model"]
                }
                else if (map["collection"]) {
                    assert !map["bean"] : "'bean' cannot be used with 'collection' in render"

                    template["collection"] = map["collection"]
                }
                else if (map["bean"]) {
                    template["bean"] = map["bean"]
                }
            }
            else {
                assert false : "'template' attribute must be provided."
            }
        }
    }

    /**
     * Adds the properties common to controllers and tag libraries (and
     * anything else HTTP based) to objects of the given class.
     * @param clazz The class to add the properties to.
     */
    static void addCommonWebProperties(Class clazz) {
        mockAttributeAccess(MockHttpSession)
        def mockRequest = new GrailsMockHttpServletRequest()
        def mockResponse = new GrailsMockHttpServletResponse()
        def mockSession = new MockHttpSession()
        def mockParams = [:]
        def mockFlash = [:]

        clazz.metaClass.getRequest = {-> mockRequest}
        clazz.metaClass.getResponse = {-> mockResponse}
        clazz.metaClass.getSession = {-> mockSession}
        clazz.metaClass.getParams = {-> mockParams}
        clazz.metaClass.getFlash = {-> mockFlash}

        // Provide access to "g" taglib namespace.
        clazz.metaClass.getG = {-> return delegate }
    }

    /**
     * Mocks a command object class by adding a "validate()" method and
     * errors-related methods, like "getErrors()" and "hasErrors()".
     */
    static void mockCommandObject(Class clazz) {
        addValidateMethod(clazz)
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
    static void mockLogging(Class clazz, boolean enableDebug = false) {
        // Get the name of the class + the last component of the package
        // (if it the class is in a package).
        def pos = clazz.name.lastIndexOf('.')
        if (pos != -1) pos = clazz.name.lastIndexOf('.', pos - 1)
        def shortName = clazz.name.substring(pos + 1)

        // Dynamically inject a mock logger that simply prints the
        // log message (and optional exception) to stdout.
        def mockLogger = [
                fatal: { String msg, Throwable t = null ->
                    println "FATAL (${shortName}): $msg"
                    if (t) {
                        println "       Exception thrown - ${t.message}"
                    }
                },
                error: { String msg, Throwable t = null ->
                    println "ERROR (${shortName}): $msg"
                    if (t) {
                        println "       Exception thrown - ${t.message}"
                    }
                },
                warn:  { String msg, Throwable t = null ->
                    println "WARN (${shortName}):  $msg"
                    if (t) {
                        println "       Exception thrown - ${t.message}"
                    }
                },
                info:  { String msg, Throwable t = null ->
                    println "INFO (${shortName}):  $msg"
                    if (t) {
                        println "       Exception thrown - ${t.message}"
                    }
                },
                debug: enableDebug ? { String msg, Throwable t = null ->
                    println "DEBUG (${shortName}): $msg"
                    if (t) {
                        println "       Exception thrown - ${t.message}"
                    }
                } : { String msg, Throwable t = null -> },
                trace: { String msg, Throwable t = null -> } ]
        clazz.metaClass.getLog = {-> mockLogger }
    }

    /**
     * Call this to mock the given domain class. It adds mock versions
     * of the various static and instance methods that are normally
     * injected by Grails. The methods behave as if there is no data
     * in the database, i.e. <code>DomainClass.list()</code> would
     * return an empty list.
     * @param clazz The domain class to mock.
     */
    static void mockDomain(Class clazz) {
        mockDomain(clazz, [])
    }

    /**
     * <p>Call this to mock the given domain class. It adds mock versions
     * of the various static and instance methods that are normally
     * injected by Grails. The methods behave as if the given list of
     * domain instances are already in the database. In fact, the finders
     * and other query methods return the instances in the same order as
     * they appear in the list <code>testInstances</code>. This makes
     * testing much easier as you can rely on this ordering.</p>
     * @param clazz The domain class to mock.
     * @param testInstances A list of instances of type <code>clazz</code>
     * or of anything that can act as that type, such as a map with keys
     * that match the domain class's fields.
     */
    static void mockDomain(Class clazz, List testInstances) {
        addDynamicFinders(clazz, testInstances)
        addGetMethods(clazz, testInstances)
        addListMethod(clazz, testInstances)
        addDynamicInstanceMethods(clazz, testInstances)
        addValidateMethod(clazz, testInstances)

        // Note that if the test instances are of type "clazz", they
        // will not have the extra dynamic methods because they were
        // created before the methods were added to the class.
        //
        // So, for each test object that is an instance of "clazz", we
        // manually change its metaclass to "clazz"'s so that it gets
        // the extra methods.
        testInstances?.eachWithIndex { obj, i ->
            if (obj.metaClass.hasProperty(obj, "id") && !obj.id) {
                obj.id = i + 1
            }

            if (clazz.isAssignableFrom(obj.getClass())) {
                obj.metaClass = clazz.metaClass
            }
        }
    }

    /**
     * Adds a <code>validate()</code> method to the given domain class
     * that performs validation against the constraints and returns a
     * map of errors. Each key in the map is the name of a field with
     * at least one error, while the value is the name of the constraint
     * that triggered the error, e.g. "nullable", or "min".
     * @param clazz The domain class to mock.
     */
    static void prepareForConstraintsTests(Class clazz) {
        prepareForConstraintsTests(clazz, [])
    }


    /**
     * <p>Adds a <code>validate()</code> method to the given domain class
     * that performs validation against the constraints and returns a
     * map of errors. Each key in the map is the name of a field with
     * at least one error, while the value is the name of the constraint
     * that triggered the error, e.g. "nullable", or "min".</p>
     * <p>The main use for this version of the method is to test unique
     * constraints. The domain instance being validated will be checked
     * against the given list of objects and if a uniqueness constraint
     * is violated, it will appear in the returned list of errors.</p>
     * @param clazz The domain class to mock.
     * @param testInstances A list of instances of type <code>clazz</code>
     * or of anything that can act as that type, such as a map with keys
     * that match the domain class's fields. In fact, the instances only
     * need properties that match the fields taking part in the unique
     * constraints.
     */
    static void prepareForConstraintsTests(Class clazz, List testInstances) {
        addValidateMethod(clazz, testInstances)
    }

    private static void addDynamicFinders(Class clazz, List testInstances) {
        // Implement the dynamic class methods for domain classes.
        clazz.metaClass.'static'.methodMissing = { method, args ->
            def m = method =~ /^find(All)?By${DYNAMIC_FINDER_RE}$/
            if (m) {
                def field = Introspector.decapitalize(m[0][2])
                def comparator = m[0][3]

                // How many arguments do we need to pass for the given
                // comparator?
                def numArgs = getArgCountForComparator(comparator)

                // Strip out that number of arguments from the ones
                // we've been passed.
                def subArgs = args[0..<numArgs]
                def result = processInstances(testInstances, field, comparator, subArgs)

                args = args[numArgs..<args.size()]

                // If we have a second clause, evaluate it now.
                def join = m[0][5]
                if (join) {
                    field = Introspector.decapitalize(m[0][6])
                    comparator = m[0][7]
                    numArgs = getArgCountForComparator(comparator)
                    subArgs = args[0..<numArgs]

                    def secondResult = processInstances(testInstances, field, comparator, subArgs)

                    args = args[numArgs..<args.size()]

                    // Combine the first result with the second result
                    // based on the join type.
                    if (join == "And") {
                        result = intersect(result, secondResult)
                    }
                    else if (join == "Or") {
                        result = intersect(testInstances, result + secondResult)
                    }
                    else {
                        throw RuntimeException("Unrecognised join type: '$join'")
                    }
                }

                // Check whether we have any options, such as "sort".
                if (args) {
                    assert args[0] instanceof Map
                    def params = args[0]
                    if (params["sort"]) {
                        result = result.sort { it."${params['sort']}" }
                    }
                }

                if (m[0][1]) {
                    // We're doing a findAllBy* so return a list.
                    return result ?: []
                }
                else {
                    // we're doing a findBy* so just return the first
                    // result (or null if there are none).
                    return result ? result[0] : null
                }
            }
        }
    }

    private static void addGetMethods(Class clazz, List testInstances) {
        // First get()...
        clazz.metaClass.'static'.get = { id ->
            id = id?.toLong()
            return testInstances.find { it?.id == id }
        }

        // ...and then getAll()
        clazz.metaClass.'static'.getAll = { Object[] args ->
            def idList = args
            if (args.length == 1 && (args[0] instanceof List || args[0].getClass().array)) {
                idList = args[0]
            }

            idList = idList?.collect { it?.toLong() }
            return idList?.collect {id -> testInstances.find { it.id == id }}?.findAll { it != null }
        }

        clazz.metaClass.'static'.getAll = { List idList ->
            idList = idList?.collect { it?.toLong() }
            return idList?.collect {id -> testInstances.find { it.id == id }}?.findAll { it != null }
        }
    }

    private static void addListMethod(Class clazz, List testInstances) {
        // Implement the dynamic class methods for domain classes.
        clazz.metaClass.'static'.list = {
            return testInstances
        }
    }

    private static void addDynamicInstanceMethods(Class clazz, List testInstances) {
        // Add save() method.
        clazz.metaClass.save = { Map args = [:] ->
            if(validate()) {
                // The object passes validation, so to confirm that it
                // has been saved we add it to the list of test instances.
                testInstances << delegate
                return delegate
            }
            return null
        }

        // Add delete() method.
        clazz.metaClass.delete = { Map args = [:] ->
            for (int i in 0..<testInstances.size()) {
                if (testInstances[i] == delegate) {
                    testInstances[i] = null
                    break;
                }
            }
        }

        // Add the "addTo*" and "removeFrom*" methods.
        Introspector.getBeanInfo(clazz).propertyDescriptors.each { PropertyDescriptor pd ->
            if (Collection.isAssignableFrom(pd.propertyType)) {
                // Capitalise the name of the property.
                def propertyName = pd.name
                def collectionName = propertyName[0].toUpperCase() + propertyName[1..-1]

                clazz.metaClass."addTo${collectionName}" = { arg ->
                    def obj = delegate
                    if (obj."${propertyName}" == null) {
                        obj."${propertyName}" = GrailsClassUtils.createConcreteCollection(pd.propertyType)
                    }

                    def prop = obj."${propertyName}"
                    prop << arg
                    return obj
                }

                clazz.metaClass."removeFrom${collectionName}" = { arg ->
                    delegate."${propertyName}"?.remove(arg)
                    return delegate
                }
            }
        }
    }

    /**
     * Adds a <code>validate()</code> method to the given domain class
     * or command object. It also adds the errors-related methods and
     * data-binding support (via the constructor and the <code>properties
     * </code> property).
     * @param clazz The domain or command class to add the methods to.
     * @param testInstances An optional list of existing instances of
     * the associated class, which is used to test the "unique" constraint.
     */
    private static void addValidateMethod(Class clazz, List testInstances = []) {
        // Read the constraints.
        def constraintsBuilder = new ConstrainedPropertyBuilder(clazz.newInstance())
        def c = GrailsClassUtils.getStaticPropertyValue(clazz, "constraints")

        if (c) {
            c.delegate = constraintsBuilder
            c.call()
        }

        // Attach the instantiated constraints to the domain/command
        // object.
        clazz.metaClass.getConstraints = {->
            constraintsBuilder.constrainedProperties
        }

        // Add data binding capabilities
        def constructor = new org.codehaus.groovy.grails.web.metaclass.DataBindingDynamicConstructor()
        clazz.metaClass.constructor =  { Map params ->
            constructor.invoke(clazz, [params] as Object[])
        }

        def setProps = new org.codehaus.groovy.grails.web.metaclass.SetPropertiesDynamicProperty()
        clazz.metaClass.setProperties = {Object o ->
            setProps.set(delegate, o)
        }

        clazz.metaClass.getProperties = {->
            new org.codehaus.groovy.grails.commons.metaclass.LazyMetaPropertyMap(delegate)
        }

        // Add all the errors-related methods.
        clazz.metaClass.getErrors = {-> return getErrorsFor(delegate) }
        clazz.metaClass.hasErrors = {-> return getErrorsFor(delegate).hasErrors() }
        clazz.metaClass.setErrors = { Errors errors -> setErrorsFor(delegate, errors) }
        clazz.metaClass.clearErrors = {-> clearErrorsFor(delegate) }

        // Finally add the "validate()" method, which can simply be
        // used to test the constraints or used from code under test.
        clazz.metaClass.validate = {
            def obj = delegate

            // Make sure any existing errors are cleared.
            clearErrors()

            constraintsBuilder.constrainedProperties.each { property, constraint ->
                constraint.validate(obj, obj."${property}", errors)
                // Handle the unique constraint if this field has one.
                def uniqueValue = constraint.getMetaConstraintValue("unique")
                if (uniqueValue) {
                    def props
                    if (uniqueValue instanceof Boolean && uniqueValue) {
                        props = []
                    }
                    else if (uniqueValue instanceof String) {
                        props = [ uniqueValue ]
                    }
                    else if (uniqueValue instanceof Collection) {
                        props = uniqueValue
                    }

                    props = [ property ] + props
                    def existing = testInstances.find { inst -> props.every { inst."$it" == obj."$it" } }
                    if (existing != null) {
                        errors.rejectValue(property, "unique")
                    }
                }
            }
            return !errors.hasErrors()
        }
    }

    private static processInstances(instances, property, comparator, args) {
        def result = []
        instances.each { record ->
            def propValue = record."${property}"
            switch(comparator) {
            case null:
                if (propValue == args[0]) result << record
                break

            case "IsNull":
                if (propValue == null) result << record
                break

            case "IsNotNull":
                if (propValue != null) result << record
                break

            case "LessThan":
                if (propValue < args[0]) result << record
                break

            case "LessThanEquals":
                if (propValue <= args[0]) result << record
                break

            case "GreaterThan":
                if (propValue > args[0]) result << record
                break

            case "GreaterThanEquals":
                if (propValue >= args[0]) result << record
                break

            case "NotEqual":
                if (propValue != args[0]) result << record
                break

            case "Like":
                if (propValue ==~ args[0].replaceAll("%", ".*")) result << record
                break

            case "Ilike":
                if (propValue ==~ /(?i)${args[0].replaceAll("%", ".*")}/) result << record
                break;

            case "Between":
                if (propValue >= args[0] && propValue <= args[1]) result << record
                break;

            default:
                throw new RuntimeException("Unrecognised comparator: ${comparator}")
            }
        }

        return result
    }

    private static int getArgCountForComparator(String comparator) {
        if (comparator == "Between") {
            return 2
        }
        else if (["IsNull", "IsNotNull"].contains(comparator)) {
            return 0
        }
        else {
            return 1
        }
    }

    /**
     * Returns a list of all the items that are in both <code>left</code>
     * and <code>right</code>. The items in the returned list have the
     * same order as the items in <code>left</code>.
     */
    private static List intersect(List left, List right) {
        def result = new ArrayList(left.size())
        left.each { item ->
            if (right.contains(item)) result << item
        }

        return result
    }

    /**
     * Gets the errors instance for an object if one exists, or creates a
     * new one otherwise. The errors instances are stored in a thread-local
     * map.
     * @param object The object whose errors instance should be retrieved.
     * @param replaceExisting <code>true</code> if any existing errors
     * instance should be replaced by a fresh one.
     * @return the Errors instance attached to the object.
     */
    private static Errors getErrorsFor(object, boolean replaceExisting = false) {
        // Check whether there is already an errors object for this
        // command object. If so, use that...
        def errors = errorsObjects.get()[object]

        // ...otherwise create a new one and store it in the thread
        // local map.
        if (!errors || replaceExisting) {
            // This takes advantage of the fact that "clearErrorsFor()"
            // creates a new, empty Errors instance.
            errors = clearErrorsFor(object)
        }

        return errors
    }

    /**
     * Sets the errors instance for an object. The errors instances are
     * stored in a thread-local map.
     * @param object The object whose errors instance should be changed.
     * @param errors The new Errors instance for the object.
     */
    private static void setErrorsFor(object, Errors errors) {
        errorsObjects.get()[object] = errors
    }

    /**
     * Clears any errors that might be attached to the given object.
     * What it really does is replace the errors instance in the
     * thread-local map with a completely new one.
     * @param object The object to clear the errors from.
     * @return the new, empty Errors instance attached to the object.
     */
    private static Errors clearErrorsFor(object) {
        Errors errors = new GrailsMockErrors(object)
        errorsObjects.get()[object] = errors
        return errors
    }
}

/**
 * Custom ThreadLocal that stores a weak hash map. This is the map that
 * we use to associate command objects/domain instances with their
 * corresponding errors objects.
 */
class ThreadLocalMap extends ThreadLocal {
    protected initialValue() {
        return new WeakHashMap()
    }
}
