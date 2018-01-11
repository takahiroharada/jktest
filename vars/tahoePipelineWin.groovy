
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
    def retNode = {
    	node("windows")
    	{
	    	stage("Check")
	        {
	            checkOutBranchOrScm(projectBranch, 'https://github.com/takahiroharada/firerender.git')
	        }
	        stage("Build") 
	        {
	            try 
	            {
	            	bat './scripts/build/win/buildTahoeMin.bat'
	            }
	            finally {
	            }
	        }
	        stage("Test")
	        {
	        	bat '''
                cd ./scripts/test/
                runFuncTests.bat
                '''
	        }
	        stage("Artifact")
	        {
	        	archiveArtifacts artifacts: 'dist/release/**/*'
	        }
	    }
    }
    return retNode
}

def executeBuilds(String projectBranch)
{
    def tasks = [:]

    tasks["Windows"] = executeBuildWin(projectBranch)

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
        }
    }
    finally 
    {
    }
}
