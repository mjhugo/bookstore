class SearchController {
    def searchService
    def query = {
        return [results:searchService.findBook(params.title)]
    }
}
