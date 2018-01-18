
def checkOutBranchOrScm(String branchName, String repoName) {
    if(branchName != "")
    {
        echo "checkout from user branch: ${branchName}; repo: ${repoName}"
        checkout([$class: 'GitSCM', branches: [[name: "*/${branchName}"]], doGenerateSubmoduleConfigurations: false, extensions: [
            [$class: 'CleanCheckout'],
            [$class: 'CheckoutOption', timeout: 30],
            [$class: 'CloneOption', timeout: 30, noTags: false],
            [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: false, recursiveSubmodules: true, reference: '', trackingSubmodules: false]
            ], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'radeonprorender', url: "${repoName}"]]])
    }
    else
    {
        echo 'checkout from scm options'
	    checkout([
	        $class: 'GitSCM',
	        branches: scm.branches,
	        doGenerateSubmoduleConfigurations: false,
	        extensions: scm.extensions + [[$class: 'SubmoduleOption', parentCredentials: true]],
	        userRemoteConfigs: scm.userRemoteConfigs
	    ])
    }
}

def executeBuildWin(String projectBranch)
{
	node("win10" && "git")
	{
    	stage("Check")
        {
            checkOutBranchOrScm(projectBranch, 'https://github.com/takahiroharada/firerender.git')
        }
        stage("Build") 
        {
            try 
            {
            	bat './scripts/build/win/buildTahoe.bat'
            }
            finally {
            }
            stash includes: 'dist/**/*', name: 'binaries'
            stash includes: 'Resources/**/*', name: 'resources'
            stash includes: 'scripts/**/*', name: 'scripts'
        }
    }
}


def executeBuilds(String projectBranch)
{
    executeBuildWin(projectBranch)
/*
    def tasks = [:]    
    testPlatforms.split(';').each()
    {
        tasks["${it}"] = executeTestWindows("${it}", projectBranch)
    }
    node("gpu${asicName}")
    {
	
    }
*/    
}

def executeTestsCpu(String projectBranch)
{
    def retNode = {
        node("win10")
        {
            stage("Test")
            {
                unstash 'binaries'
                unstash 'resources'
                unstash 'scripts'

                bat './scripts/test/win/tahoeTestsCpu.bat'
            }
            stage("Artifact")
            {
                archiveArtifacts artifacts: 'dist/release/**/*'
                junit 'scripts/*.xml'
            }
        }
    }
    return retNode
}

def executeTestsGpu(String projectBranch, String gpu)
{
    def retNode = {
        node("win10" && gpu)
        {
            stage("Test")
            {
                unstash 'binaries'
                unstash 'resources'
                unstash 'scripts'

                bat './scripts/test/win/tahoeTestsGpu.bat'
            }
            stage("Artifact")
            {
                archiveArtifacts artifacts: 'dist/release/**/*'
                junit 'scripts/*.xml'
            }
        }
    }
    return retNode
}

def executeTests(String projectBranch)
{
    def tasks = [:]

	String gpus = "vega,fiji,quadrok5000,geforce1080"

    tasks["TestCpu"] = executeTestsCpu(projectBranch)
	gpus.split(',').each()
	{
		gpu = "${it}"
		tasks[gpu] = executeTestsGpu(projectBranch,gpu)
	}
//	tasks["Test fiji"] = executeTestsGpu(projectBranch,"fiji")
//    tasks["Test quadrok5000"] = executeTestsGpu(projectBranch,"quadrok5000")
//    tasks["Test geforce1080"] = executeTestsGpu(projectBranch,"geforce1080")

    parallel tasks
/*
    def tasks = [:]    
    testPlatforms.split(';').each()
    {
        tasks["${it}"] = executeTestWindows("${it}", projectBranch)
    }
    node("gpu${asicName}")
    {
    
    }
*/    
}

def call(String projectBranch='', String testPlatforms = 'AMD_RXVEGA;AMD_WX9100;AMD_WX7100', Boolean enableNotifications = true) {
      
    try 
    {
        timestamps {
            executeBuilds(projectBranch)
            executeTests(projectBranch)
        }
    }
    finally 
    {
    }
}
