class PublisherTests extends grails.test.GrailsUnitTestCase {

    Publisher publisher

    void setUp() {
        super.setUp()

        // in testing-plugin 0.4 you can just do "mockForConstraintsTests(Book)" instead of these two lines
        registerMetaClass(Publisher)
        grails.test.MockUtils.prepareForConstraintsTests(Publisher, [new Publisher(name:'Appress')])

        publisher = new Publisher(name: 'Manning')
        assertTrue publisher.validate()
    }

    void testNameConstraint(){
        publisher.name = 'Appress'
        assertFalse publisher.validate()
        assertEquals "unique", publisher.errors["name"]        
    }


}
