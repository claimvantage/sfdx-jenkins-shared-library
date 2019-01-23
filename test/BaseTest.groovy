import org.junit.*
import com.lesfurets.jenkins.unit.*
import static groovy.test.GroovyAssert.*

class BaseTest extends BasePipelineTest {
    @Before
    void setUp() {
        super.setUp()

        // load all steps from vars directory
        new File("vars").eachFile { file ->
            def name = file.name.replace(".groovy", "")

            // register step with no args, example: retrieveExternals()
            helper.registerAllowedMethod(name, []) { ->
                loadScript(file.path)()
            }

            // register step with Map arg, example: sfdxBuildPipeline(glob: "a")
            helper.registerAllowedMethod(name, [ Map ]) { opts ->
                loadScript(file.path)(opts)
            }
            
            // register step with Org arg, example: createScratchOrg(org)
            helper.registerAllowedMethod(name, [ com.claimvantage.sjsl.Org ]) { org ->
                loadScript(file.path)(org)
            }
            
            // register step with String arg, example: shWithResult("sfdx")
            helper.registerAllowedMethod(name, [ String ]) { s ->
                loadScript(file.path)(s)
            }
            
            // register step with Map and Closure args, example: withOrgsInParallel([:], {})
            helper.registerAllowedMethod(name, [Map, Closure]) { Map m, Closure c ->
                loadScript(file.path)(m, c)
            }
        }
    }
}
