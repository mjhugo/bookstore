class Book {

    String title
    Date publishedDate
    Integer pages

    Author primaryAuthor

    static constraints = {
        title(nullable:true, blank:false)
        publishedDate(nullable:true)
        pages(min:0)
        primaryAuthor(nullable:true)
    }

}
