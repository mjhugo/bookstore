class BookTagLib {
    def formatTitle = {attrs ->
        out << "<u>${attrs.title}</u>"
    }
}
