class PublisherController {
    
    def index = { redirect(action:list,params:params) }

    // the delete, save and update actions only accept POST requests
    def allowedMethods = [delete:'POST', save:'POST', update:'POST']

    def list = {
        if(!params.max) params.max = 10
        [ publisherList: Publisher.list( params ) ]
    }

    def show = {
        def publisher = Publisher.get( params.id )

        if(!publisher) {
            flash.message = "Publisher not found with id ${params.id}"
            redirect(action:list)
        }
        else { return [ publisher : publisher ] }
    }

    def delete = {
        def publisher = Publisher.get( params.id )
        if(publisher) {
            publisher.delete()
            flash.message = "Publisher ${params.id} deleted"
            redirect(action:list)
        }
        else {
            flash.message = "Publisher not found with id ${params.id}"
            redirect(action:list)
        }
    }

    def edit = {
        def publisher = Publisher.get( params.id )

        if(!publisher) {
            flash.message = "Publisher not found with id ${params.id}"
            redirect(action:list)
        }
        else {
            return [ publisher : publisher ]
        }
    }

    def update = {
        def publisher = Publisher.get( params.id )
        if(publisher) {
            publisher.properties = params
            if(!publisher.hasErrors() && publisher.save()) {
                flash.message = "Publisher ${params.id} updated"
                redirect(action:show,id:publisher.id)
            }
            else {
                render(view:'edit',model:[publisher:publisher])
            }
        }
        else {
            flash.message = "Publisher not found with id ${params.id}"
            redirect(action:edit,id:params.id)
        }
    }

    def create = {
        def publisher = new Publisher()
        publisher.properties = params
        return ['publisher':publisher]
    }

    def save = {
        def publisher = new Publisher(params)
        if(!publisher.hasErrors() && publisher.save()) {
            flash.message = "Publisher ${publisher.id} created"
            redirect(action:show,id:publisher.id)
        }
        else {
            render(view:'create',model:[publisher:publisher])
        }
    }
}
