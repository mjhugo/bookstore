class BookTagLibTests extends grails.test.TagLibUnitTestCase {

    public void setUp() {
        super.setUp();
        // not necessary after upgrade to testing plugin 0.4
        tagLib = new BookTagLib()
    }

    void testFormatTitle(){
        String title = 'To Kill a Mocking Bird'
        def resultBuffer = tagLib.formatTitle([title:title])
        assertEquals "<u>${title}</u>", resultBuffer.toString()
    }

    // not necessary after upgrade to testing plugin 0.4
    protected void reset() {
        mockRequest.clearAttributes()
        mockRequest.removeAllParameters()
        mockResponse.committed = false
        mockSession.clearAttributes()
        mockSession.setNew(true)

        renderArgs.clear()
        mockParams.clear()
        mockFlash.clear()
    }

    // not necessary after upgrade to testing plugin 0.4
    protected newInstance() {
        def instance = this.testClass.newInstance()

        renderArgs = instance.renderArgs
        mockRequest = instance.request
        mockResponse = instance.response
        mockSession = instance.session

        mockParams = instance.params
        mockFlash = instance.flash
    }
    
}