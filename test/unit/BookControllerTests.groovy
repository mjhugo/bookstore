class BookControllerTests extends grails.test.ControllerUnitTestCase {

    void setUp(){
        super.setUp()
        // fix for testing-plugin-0.3 (resolved in plugin version 0.4)
        controller = new BookController()
    }    

    void testIndex_withParams() {
        controller.params.max = 1
        controller.index()
        assertEquals('redirected action should match',
                controller.list,
                redirectArgs.action)
        assertEquals('params match', 1, redirectArgs.params.max)
    }

    void testList() {
        List bookList = []
        20.times {i -> bookList << new Book() }
        
        // setup a mock control to check the params passed to the list method
        def mockControl = mockFor(Book)
        mockControl.demand.static.list {params ->
            assert params.max == 10
            return bookList[0..9]
        }

        Map model = controller.list()
        assertNotNull('list should be present in model', model.bookList)
        assertEquals('list size should match default', 10, model.bookList.size())

        // verify the mocked method was called
        mockControl.verify()
    }

    void testList_withParams() {
        List bookList = []
        20.times {i -> bookList << new Book() }

        // setup a mock control to check the params passed to the list method
        def mockControl = mockFor(Book)
        Integer MAX = bookList.size()
        mockControl.demand.static.list {params ->
            assert params.max == MAX
            return bookList
        }

        controller.params.max = MAX
        def model = controller.list()
        assertNotNull('list should be present in model', model.bookList)
        assertEquals('list size should match', MAX, model.bookList.size())

        // verify the mocked method was called
        mockControl.verify()
    }

    void testShow() {
        Book expectedBook = new Book()
        mockDomain(Book, [expectedBook])
        controller.params.id = 1
        Map model = controller.show()
        assertEquals(expectedBook, model.book)
    }

    void testShow_bookNotFound() {
        mockDomain(Book)
        controller.params.id = 999 // no object with id 999 exists
        controller.show()

        assertEquals('flash message',
                "Book not found with id ${controller.params.id}",
                mockFlash.message)
        assertEquals('redirect action',
                controller.list, redirectArgs.action)
    }

    void testDelete() {
        mockDomain(Book, [new Book()])
        controller.params.id = 1
        controller.delete()
        assertEquals('redirect action', controller.list, redirectArgs.action)
        assertEquals('flash message', "Book ${controller.params.id} deleted", mockFlash.message)
    }

    void testDelete_bookNotFound() {
        mockDomain(Book)
        controller.params.id = 999 // no object with id 999 exists
        controller.delete()
        assertEquals('redirect action', controller.list, redirectArgs.action)
        assertEquals('flash message', "Book not found with id ${controller.params.id}", mockFlash.message)
    }

    void testEdit() {
        Book expectedBook = new Book()
        mockDomain(Book, [expectedBook])
        controller.params.id = 1
        Map model = controller.edit()
        assertEquals('model.book', expectedBook, model.book)
    }

    void testEdit_bookNotFound() {
        mockDomain(Book)
        controller.params.id = 999 // no object with id 999 exists
        controller.edit()
        assertEquals('redirect action', controller.list, redirectArgs.action)
        assertEquals('flash message', "Book not found with id ${controller.params.id}", mockFlash.message)
    }

    void testUpdate() {
        Book expectedBook = new Book()
        mockDomain(Book, [expectedBook])

        // mock out save and hasErrors behavior for happy path
        expectedBook.metaClass.save = {-> return true }
        expectedBook.metaClass.hasErrors = {-> return false }

        controller.params.id = 1
        controller.update()

        assertEquals('redirect action', controller.show, redirectArgs.action)
        assertEquals('redirect id', controller.params.id, redirectArgs.id)
        assertEquals('flash message', "Book ${controller.params.id} updated", mockFlash.message)
    }

    void testUpdate_hasErrors() {
        Book expectedBook = new Book()
        mockDomain(Book, [expectedBook])

        // mock out save and hasErrors behavior for non-happy path
        expectedBook.metaClass.save = {-> return false }
        expectedBook.metaClass.hasErrors = {-> return false }

        controller.params.id = 1
        controller.update()

        assertEquals('render view', 'edit', renderArgs.view)
        assertEquals('render model book', Book.get(controller.params.id), renderArgs.model.book)
    }

    void testUpdate_bookNotFound() {
        mockDomain(Book)
        controller.params.id = 999 // no object with id 999 exists
        controller.update()
        assertEquals('redirect action', controller.edit, redirectArgs.action)
        assertEquals('flash message', "Book not found with id ${controller.params.id}", mockFlash.message)
    }

    void testCreate() {
        mockDomain(Book)
        Map model = controller.create()
        assertNotNull('book', model.book)
    }

    void testSave() {
        mockDomain(Book)
        // mock out save method for happy path
        Book.metaClass.save = {-> return true}

        controller.save()

        assertEquals('redirect action', controller.show, redirectArgs.action)
        assertEquals('redirect id', controller.params.id, redirectArgs.id)
        assertEquals('flash message', "Book ${controller.params.id} created", mockFlash.message)
    }

    void testSave_withErrors() {
        mockDomain(Book)
        // mock out save method for non-happy path
        Book.metaClass.save = {-> return false}

        controller.save()

        assertEquals('render view', 'create', renderArgs.view)
        assertNotNull('book', renderArgs.model.book)
    }

}
