import org.junit.*
import static groovy.test.GroovyAssert.*

// Testing simple calls to withOrgsInParallel step using string and map
// Currently is basically checking for compilation and can be used for manual inspection of call stack
class WithOrgInParallelTest extends BaseTest {
    def withOrgsInParallel
    def glob = ['master': 'config/project-scratch-def*.json'].withDefault{'config/project-scratch-def.json'}

    @Before
    void setUp() {
        super.setUp()

        withOrgsInParallel = loadScript("vars/withOrgsInParallel.groovy")
    }

    @Test
    void callStep() {
        withOrgsInParallel()
        printCallStack()
    }

    @Test
    void callStepWithScratchOrgDef() {
        withOrgsInParallel(glob: 'config/project-scratch-def.json')
        printCallStack()
    }

    @Test
    void callStepWithMapOnMasterBranch() {
        mockBranchName('master')

        withOrgsInParallel(glob: glob)
        printCallStack()
    }

    @Test
    void callStepWithMapOnPrBranch() {
        mockBranchName('PR')

        withOrgsInParallel(glob: glob)
        printCallStack()
    }

    private void mockBranchName(branchName) {
        binding.setVariable("env", [BRANCH_NAME: branchName])
    }
}