
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
        checkout scm
    }
}

def executeBuildWindowsVS2015(String projectBranch)
{
    def retNode = {
    	stage("Check")
        {
        	sh 'ls ./'
        }
        stage("Build") 
        {
            checkOutBranchOrScm(projectBranch, 'https://github.com/takahiroharada/firerender.git')

            try 
            {
            	sh './scripts/build/macos/buildTahoeMin.sh'
            }
            finally {
            }
        }
        stage("Test")
        {
        	sh 'ls ./dist/release/bin/x86_64/'
        }
    }
    return retNode
}

def executeBuilds(String projectBranch)
{
    def tasks = [:]

    tasks["Windows"] = executeBuildWindowsVS2015(projectBranch)

    parallel tasks
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
