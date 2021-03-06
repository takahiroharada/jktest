def executeBuildsImpl(String os, String commandLinux, String commandWin,
    def checkoutFunc, def postbuildFunc )
{
    def retNode = {
	node("${os} && git")
	{
    	stage("Check-"+os)
        {
            checkoutFunc()
        }
        stage("Build-"+os) 
        {
            try 
            {
                if( isUnix() )
                {
                    sh commandLinux
                }
                else
                {
                	bat commandWin
                }
            }
            finally {
            }
            postbuildFunc( os )
        }
    }
    }
    return retNode
}

def executeTestsImpl(String os, String gpu, 
    String testCommandCpu, String testCommandGpu, 
    String testCommandLinuxCpu, String testCommandLinuxGpu, 
    def pretestFunc, def deployFunc )
{
    def retNode = {
        node("${os} && ${gpu}")
        {
            stage("Test-" + gpu)
            {
                pretestFunc( os )
                if( gpu == "cpu" )
                {
                    if( isUnix() )
                        sh testCommandLinuxCpu
                    else
                        bat testCommandCpu
                }
                else
                {
                    if( isUnix() )
                        sh testCommandLinuxGpu
                    else
                        bat testCommandGpu
                }
            }
            stage("Artifact-"+gpu)
            {
                deployFunc()
            }
        }
    }
    return retNode
}

def executeBuilds(String oses, String commandLinux, String commandWin, 
    def checkoutFunc, def postbuildFunc )
{
    def tasks = [:]

    oses.split(',').each()
    {
        String os = "${it}"
        tasks["Build-"+os] = executeBuildsImpl(os, commandLinux, commandWin, checkoutFunc, postbuildFunc )

    }

    parallel tasks
}

def executeTests(String testPlatforms, 
    String testCmdWinCpu, String testCmdWinGpu, 
    String testCmdLinuxCpu, String testCmdLinuxGpu,
    def pretestFunc, def deployFunc )
{
    def tasks = [:]

    testPlatforms.split(',').each()
    {
        def (os, gpu) = it.tokenize(':')
        tasks[os+"-"+gpu] = executeTestsImpl( os, gpu, 
            testCmdWinCpu, testCmdWinGpu, testCmdLinuxCpu, testCmdLinuxGpu, 
            pretestFunc, deployFunc )        
    }

    parallel tasks
}

def checkoutImpl()
{
	checkout scm
    dir('deps')
    {
        git credentialsId: 'f5c0b9de-fe25-43b6-859f-5b09c96050f4', url: 'https://github.com/amdadvtech/firerenderdeps.git'
    }

    dir('testdata')
    {
        git credentialsId: 'f5c0b9de-fe25-43b6-859f-5b09c96050f4', url: 'https://github.com/amdadvtech/frunittestdata.git'
    }

    if( isUnix() )
    {
        sh 'cp -r ./deps/contrib ./'
        sh 'ls contrib/lib/linux64'
        sh 'cp -r ./testdata/tahoe ./'
        sh 'cp -r ./testdata/unittestdata ./'
    }
    else
    {
        bat 'xcopy /E/Y deps\\contrib contrib'
        bat 'xcopy /E/Y testdata .'
    }
}

def postBuildImpl( String os )
{
    stash includes: 'dist/**/*', name: 'binaries'+os
    stash includes: 'Resources/**/*', name: 'resources'+os
    stash includes: 'scripts/**/*', name: 'scripts'+os   
    if( isUnix() )
        stash includes: 'tahoe/**/*.png', name: 'tahoe'+os   
    else
        stash includes: 'Tahoe/**/*.png', name: 'tahoe'+os   
    stash includes: 'unittestdata/**/*', name: 'unittestdata'+os   
}

def pretestImpl( String os )
{
    cleanWs()
    unstash 'binaries'+os
    unstash 'resources'+os
    unstash 'scripts'+os
    unstash 'tahoe'+os   
    unstash 'unittestdata'+os   
}

def deployImpl()
{
    archiveArtifacts artifacts: 'scripts/*.xml'
    junit 'scripts/*.xml'
}

//def call(String testOses = "win10,ubuntu", String testPlatforms = "win10:fiji,win10:ellesmere,win10:vega,ubuntu:fiji,ubuntu:ellesmere", 
def call(String testOses = "win10,ubuntu", String testPlatforms = "win10:fiji,ubuntu:fiji", 
    String buildCmdLinux = './scripts/build/macos/buildTahoe.sh',
    String buildCmdWin = './scripts/build/win/buildTahoe.bat',
    
    String testCmdWinCpu = './scripts/test/win/tahoeMinTestsCpu.bat',
    String testCmdWinGpu = './scripts/test/win/tahoeMinTestsGpu.bat',
    String testCmdLinuxCpu = './scripts/test/macos/tahoeTestsCpu.sh',
    String testCmdLinuxGpu = './scripts/test/macos/tahoeTestsGpu.sh') 
{
    try 
    {
        timestamps {
            executeBuilds( testOses, buildCmdLinux, buildCmdWin,
                this.&checkoutImpl, this.&postBuildImpl )
            executeTests(testPlatforms, testCmdWinCpu, testCmdWinGpu, testCmdLinuxCpu, testCmdLinuxGpu,
                this.&pretestImpl, this.&deployImpl )
        }
    }
    finally 
    {
    }
}
