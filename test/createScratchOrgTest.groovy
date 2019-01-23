import org.junit.*
import static groovy.test.GroovyAssert.*
import com.claimvantage.sjsl.Org

class CreateScratchOrgTest extends BaseTest {
    def createScratchOrg

    @Before
    void setUp() {
        super.setUp()

        createScratchOrg = loadScript("vars/createScratchOrg.groovy")
    }
    
    @Test
    void testCall() {
       Org org = new Org("testScratchOrg.json")
       createScratchOrg(org)
       printCallStack()
    }
