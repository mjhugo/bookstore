class SearchServiceTests extends grails.test.GrailsUnitTestCase {

    void testFindBook() {
        mockLogging(SearchService, true) //must happen before object instantiation
        SearchService service = new SearchService()

        Book tripwire = new Book(title:'Tripwire')
        Book stateOfFear = new Book(title:'State of Fear')

        mockDomain(Book, [tripwire, stateOfFear])

        def results = service.findBook(tripwire.title)

        assertEquals tripwire, results[0]
    }
}
