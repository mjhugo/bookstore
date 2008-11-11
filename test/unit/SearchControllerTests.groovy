class SearchControllerTests extends grails.test.ControllerUnitTestCase {

    void setUp(){
        super.setUp()
        // fix for testing-plugin-0.3 bug (resolved in plugin version 0.4)
        controller = new SearchController()
    }

    void testQuery(){
        def searchServiceControl = mockFor(SearchService)
        Book expectedBook = new Book(title:'A Book Title')
        searchServiceControl.demand.findBook(1) {title ->
            assert expectedBook.title == title
            return expectedBook
        }

        controller.searchService = searchServiceControl.createMock()

        controller.params.title = expectedBook.title
        Map model = controller.query()

        assertEquals(expectedBook, model.results)

        searchServiceControl.verify()
    }
}
