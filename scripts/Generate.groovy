Ant.property(environment: 'env')
grailsHome = Ant.antProject.properties.'env.GRAILS_HOME'

includeTargets << new File ("${grailsHome}/scripts/Init.groovy")

target ('default': 'generate controller unit test') {
    // Make sure any arguments have been parsed if the parser is
    // available.
    hasArgsParser = getBinding().variables.containsKey('argsMap')
    if (hasArgsParser) {
        depends(parseArguments, checkVersion)
    }
    else {
        depends(checkVersion)
    }

    def newTestFile = "test/unit/${args}ControllerTests.groovy"

    Ant.copy(file: "src/templates/ControllerTests.groovy",
            tofile: newTestFile,
            overwrite: true)

    Ant.replace(file: newTestFile) {
        Ant.replacefilter(token: '@domain.name@', value: args)
        Ant.replacefilter(token: '@domain.name.lower@', value: args.toLowerCase())
    }
}
