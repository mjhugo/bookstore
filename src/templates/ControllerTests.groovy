class @domain.name@ControllerTests extends grails.test.ControllerUnitTestCase {

    void setUp(){
        super.setUp()
        // fix for testing-plugin-0.3 (resolved in plugin version 0.4)
        controller = new @domain.name@Controller()
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
        List @domain.name.lower@List = []
        20.times {i -> @domain.name.lower@List << new @domain.name@() }
        
        // setup a mock control to check the params passed to the list method
        def mockControl = mockFor(@domain.name@)
        mockControl.demand.static.list {params ->
            assert params.max == 10
            return @domain.name.lower@List[0..9]
        }

        Map model = controller.list()
        assertNotNull('list should be present in model', model.@domain.name.lower@List)
        assertEquals('list size should match default', 10, model.@domain.name.lower@List.size())

        // verify the mocked method was called
        mockControl.verify()
    }

    void testList_withParams() {
        List @domain.name.lower@List = []
        20.times {i -> @domain.name.lower@List << new @domain.name@() }

        // setup a mock control to check the params passed to the list method
        def mockControl = mockFor(@domain.name@)
        Integer MAX = @domain.name.lower@List.size()
        mockControl.demand.static.list {params ->
            assert params.max == MAX
            return @domain.name.lower@List
        }

        controller.params.max = MAX
        def model = controller.list()
        assertNotNull('list should be present in model', model.@domain.name.lower@List)
        assertEquals('list size should match', MAX, model.@domain.name.lower@List.size())

        // verify the mocked method was called
        mockControl.verify()
    }

    void testShow() {
        @domain.name@ expected@domain.name@ = new @domain.name@()
        mockDomain(@domain.name@, [expected@domain.name@])
        controller.params.id = 1
        Map model = controller.show()
        assertEquals(expected@domain.name@, model.@domain.name.lower@)
    }

    void testShow_@domain.name.lower@NotFound() {
        mockDomain(@domain.name@)
        controller.params.id = 999 // no object with id 999 exists
        controller.show()

        assertEquals('flash message',
                "@domain.name@ not found with id ${controller.params.id}",
                mockFlash.message)
        assertEquals('redirect action',
                controller.list, redirectArgs.action)
    }

    void testDelete() {
        mockDomain(@domain.name@, [new @domain.name@()])
        controller.params.id = 1
        controller.delete()
        assertEquals('redirect action', controller.list, redirectArgs.action)
        assertEquals('flash message', "@domain.name@ ${controller.params.id} deleted", mockFlash.message)
    }

    void testDelete_@domain.name.lower@NotFound() {
        mockDomain(@domain.name@)
        controller.params.id = 999 // no object with id 999 exists
        controller.delete()
        assertEquals('redirect action', controller.list, redirectArgs.action)
        assertEquals('flash message', "@domain.name@ not found with id ${controller.params.id}", mockFlash.message)
    }

    void testEdit() {
        @domain.name@ expected@domain.name@ = new @domain.name@()
        mockDomain(@domain.name@, [expected@domain.name@])
        controller.params.id = 1
        Map model = controller.edit()
        assertEquals('model.@domain.name.lower@', expected@domain.name@, model.@domain.name.lower@)
    }

    void testEdit_@domain.name.lower@NotFound() {
        mockDomain(@domain.name@)
        controller.params.id = 999 // no object with id 999 exists
        controller.edit()
        assertEquals('redirect action', controller.list, redirectArgs.action)
        assertEquals('flash message', "@domain.name@ not found with id ${controller.params.id}", mockFlash.message)
    }

    void testUpdate() {
        @domain.name@ expected@domain.name@ = new @domain.name@()
        mockDomain(@domain.name@, [expected@domain.name@])

        // mock out save and hasErrors behavior for happy path
        expected@domain.name@.metaClass.save = {-> return true }
        expected@domain.name@.metaClass.hasErrors = {-> return false }

        controller.params.id = 1
        controller.update()

        assertEquals('redirect action', controller.show, redirectArgs.action)
        assertEquals('redirect id', controller.params.id, redirectArgs.id)
        assertEquals('flash message', "@domain.name@ ${controller.params.id} updated", mockFlash.message)
    }

    void testUpdate_hasErrors() {
        @domain.name@ expected@domain.name@ = new @domain.name@()
        mockDomain(@domain.name@, [expected@domain.name@])

        // mock out save and hasErrors behavior for non-happy path
        expected@domain.name@.metaClass.save = {-> return false }
        expected@domain.name@.metaClass.hasErrors = {-> return false }

        controller.params.id = 1
        controller.update()

        assertEquals('render view', 'edit', renderArgs.view)
        assertEquals('render model @domain.name.lower@', @domain.name@.get(controller.params.id), renderArgs.model.@domain.name.lower@)
    }

    void testUpdate_@domain.name.lower@NotFound() {
        mockDomain(@domain.name@)
        controller.params.id = 999 // no object with id 999 exists
        controller.update()
        assertEquals('redirect action', controller.edit, redirectArgs.action)
        assertEquals('flash message', "@domain.name@ not found with id ${controller.params.id}", mockFlash.message)
    }

    void testCreate() {
        mockDomain(@domain.name@)
        Map model = controller.create()
        assertNotNull('@domain.name.lower@', model.@domain.name.lower@)
    }

    void testSave() {
        mockDomain(@domain.name@)
        // mock out save method for happy path
        @domain.name@.metaClass.save = {-> return true}

        controller.save()

        assertEquals('redirect action', controller.show, redirectArgs.action)
        assertEquals('redirect id', controller.params.id, redirectArgs.id)
        assertEquals('flash message', "@domain.name@ ${controller.params.id} created", mockFlash.message)
    }

    void testSave_withErrors() {
        mockDomain(@domain.name@)
        // mock out save method for non-happy path
        @domain.name@.metaClass.save = {-> return false}

        controller.save()

        assertEquals('render view', 'create', renderArgs.view)
        assertNotNull('@domain.name.lower@', renderArgs.model.@domain.name.lower@)
    }

}
