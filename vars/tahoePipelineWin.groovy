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
}

def executeTests(String projectBranch, String gpu, String testCommandCpu, String testCommandGpu, String artifactPath)
{
    def retNode = {
        node("win10" && gpu)
        {
            stage("Test-" + gpu)
            {
                unstash 'binaries'
                unstash 'resources'
                unstash 'scripts'
                if( gpu == "cpu" )
                    bat testCommandCpu
                else
                    bat testCommandGpu
            }
            stage("Artifact")
            {
                archiveArtifacts artifacts: artifactPath
                junit 'scripts/*.xml'
            }
        }
    }
    return retNode
}

def executeTests(String projectBranch)
{
    def tasks = [:]

//	String gpus = "cpu,vega,fiji,quadrok5000,geforce1080"
    String gpus = "cpu,vega,fiji"

	gpus.split(',').each()
	{
		gpu = "${it}"
		tasks[gpu] = executeTests(projectBranch,gpu, 
            './scripts/test/win/tahoeTestsCpu.bat', './scripts/test/win/tahoeTestsGpu.bat',
            'dist/release/**/*')
	}
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
