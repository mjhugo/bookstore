class Author {

    String firstName
    String lastName

    static hasMany = [books:Book]

    static constraints = {
        firstName(nullable:true)
        lastName(blank:false)
    }

}
