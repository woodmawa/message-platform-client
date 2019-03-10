package scripts

ConfigSlurper slurper = new ConfigSlurper ()

slurper.parse ("""host='localhost' """)

println slurper.getProperty('host')

/* gives exception :
Caught: groovy.lang.MissingPropertyException: No such property: host for class: groovy.util.ConfigSlurper
groovy.lang.MissingPropertyException: No such property: host for class: groovy.util.ConfigSlurper
	at scripts.testSSlurper.run(testSSlurper.groovy:7)
 */