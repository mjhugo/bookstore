class AuthorTests extends grails.test.GrailsUnitTestCase {

    Author author

    void setUp() {
        super.setUp()

        // in testing-plugin 0.4 you can just do "mockForConstraintsTests(Book)" instead of these two lines
        registerMetaClass(Author)
        grails.test.MockUtils.prepareForConstraintsTests(Author)

        author = new Author(firstName: 'Michael', lastName: 'Scott')
        assertTrue author.validate()
    }

    void testFirsNameConstraint() {
        author.firstName = null
        assertTrue author.validate()
    }

    void testLastNameConstraint() {
        author.lastName = ''
        assertFalse author.validate()
        assertEquals "blank", author.errors["lastName"]
    }

}
