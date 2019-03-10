package scripts

ConfigSlurper slurper = new ConfigSlurper ()

def config = slurper.parse ("""host='localhost' """)

println config.getProperty('host')

/* gives exception :
Caught: groovy.lang.MissingPropertyException: No such property: host for class: groovy.util.ConfigSlurper
groovy.lang.MissingPropertyException: No such property: host for class: groovy.util.ConfigSlurper
	at scripts.testSSlurper.run(testSSlurper.groovy:7)
 */