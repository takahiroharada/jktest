
def checkOutBranchOrScm(String branchName, String repoName) {
    if(branchName != "")
    {
        echo "checkout from user branch: ${branchName}; repo: ${repoName}"
        checkout([$class: 'GitSCM', branches: [[name: "*/${branchName}"]], doGenerateSubmoduleConfigurations: false, extensions: [
            [$class: 'CleanCheckout'],
            [$class: 'CheckoutOption', timeout: 30],
            [$class: 'CloneOption', timeout: 30, noTags: false],
            [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: false, recursiveSubmodules: true, reference: '', trackingSubmodules: false]
            ], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '6fc6822a-2c5f-437d-8082-71aa452abafe', url: "${repoName}"]]])
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

def executeBuildUbuntu(String projectBranch)
{
	node("ubuntu")
	{
    	stage("Check")
        {
            checkOutBranchOrScm(projectBranch, 'https://github.com/takahiroharada/firerender.git')
//            checkOutBranchOrScm('master', 'https://github.com/amdadvtech/firerenderdeps.git')
			dir('deps')
			{
            	git credentialsId: '6fc6822a-2c5f-437d-8082-71aa452abafe', url: 'https://github.com/amdadvtech/firerenderdeps.git'
			}
			sh 'cp -r ./deps/contrib ./'
			if( env.BRANCH_NAME.contains("release") )
				sh 'echo release'
			if( env.BRANCH_NAME.contains("feature") )
				sh 'echo feature'

			sh 'ls contrib/lib/osx64'
        }
        stage("Build") 
        {
            try 
            {
            	sh './scripts/build/macos/buildTahoe.sh'
            }
            finally {
            }
            stash includes: 'dist/**/*', name: 'binaries'
            stash includes: 'Resources/**/*', name: 'resources'
            stash includes: 'scripts/**/*', name: 'scripts'
        }
    }
}

def executeTestsGpu(String projectBranch)
{
    def retNode = {
        node("ubuntu")
        {
            stage("Test")
            {
                unstash 'binaries'
                unstash 'resources'
                unstash 'scripts'

                sh './scripts/test/macos/tahoeTestsGpu.sh'
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

def executeTestsCpu(String projectBranch)
{
    def retNode = {
        node("ubuntu")
        {
            stage("Test")
            {
                unstash 'binaries'
                unstash 'resources'
                unstash 'scripts'

//                sh './scripts/test/macos/tahoeTestsCpu.sh'
				sh '''
				cd scripts
				../dist/release/bin/x86_64/UnitTest64 -cldevice cpu -referencePath ../tahoe/mstype1_GpuAmdCI+_WinLinux/ -unittestdatapath ../unittestdata/ --gtest_output=xml:resultTahoeCpu.xml --gtest_filter=*StageDemo*
				'''
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

    tasks["TestCpu"] = executeTestsCpu(projectBranch)
    tasks["TestGpu"] = executeTestsGpu(projectBranch)

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
            executeBuildUbuntu(projectBranch)
            executeTests(projectBranch)
        }
    }
    finally 
    {
    }
}
