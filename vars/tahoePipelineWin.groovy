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
            stage("Test-" + gpu)
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

def executeTests(String projectBranch, String gpu)
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
                    bat './scripts/test/win/tahoeTestsCpu.bat'
                else
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

//	String gpus = "vega,fiji,quadrok5000,geforce1080"
    String gpus = "cpu,vega,fiji"

	gpus.split(',').each()
	{
		gpu = "${it}"
		tasks[gpu] = executeTests(projectBranch,gpu)
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
