class SearchService {
    def findBook(String title) {
        log.debug "Searching for book with title: ${title}"
        return Book.findAllByTitle(title)
    }
}
