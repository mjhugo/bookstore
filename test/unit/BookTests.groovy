class BookTests extends grails.test.GrailsUnitTestCase {

    Book book

    void setUp() {
        super.setUp()

        // in testing-plugin 0.4 you can just do "mockForConstraintsTests(Book)" instead of these two lines
        registerMetaClass(Book) 
        grails.test.MockUtils.prepareForConstraintsTests(Book)

        book = new Book(title:'Office Politics',
                publishedDate: new Date(),
                pages: 0,
                primaryAuthor: new Author())
        assertTrue book.validate()
    }

    void testTitleConstraint() {
        book.title = null
        assertTrue book.validate()

        book.title = ''
        assertFalse book.validate()
        assertEquals "blank", book.errors["title"]
    }

    void testPublishedDateConstraint() {
        book.title = null
        assertTrue book.validate()
    }

    void testPagesConstraint() {
        book.pages = -2
        assertFalse book.validate()
        assertEquals "min", book.errors["pages"]
    }

    void testPrimaryAuthorConstraint() {
        book.primaryAuthor = null
        assertTrue book.validate()
    }

}
