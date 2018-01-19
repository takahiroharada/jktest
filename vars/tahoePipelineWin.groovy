def executeBuilds(String projectBranch, String os, String commandLinux, String commandWin)
{
    def retNode = {
	node("${os} && git")
	{
    	stage("Check-"+os)
        {
            checkOutBranchOrScm(projectBranch, 'https://github.com/takahiroharada/firerender.git')
            dir('deps')
            {
                git credentialsId: '6fc6822a-2c5f-437d-8082-71aa452abafe', url: 'https://github.com/amdadvtech/firerenderdeps.git'
            }

            if( isUnix() )
            {
                sh 'cp -r ./deps/contrib ./'
                sh 'ls contrib/lib/linux64'
            }
            else
            {
                bat 'xcopy /E/Y deps\\contrib contrib'
            }
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
            stash includes: 'dist/**/*', name: 'binaries'+os
            stash includes: 'Resources/**/*', name: 'resources'+os
            stash includes: 'scripts/**/*', name: 'scripts'+os
        }
    }
    }
    return retNode
}

def executeTestsImpl(String os, String gpu, 
    String testCommandCpu, String testCommandGpu, 
    String testCommandLinuxCpu, String testCommandLinuxGpu, 
    def deployFunc )
{
    def retNode = {
        node("${os} && ${gpu}")
        {
            stage("Test-" + gpu)
            {
                unstash 'binaries'+os
                unstash 'resources'+os
                unstash 'scripts'+os
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

def executeBuilds(String projectBranch, String commandLinux, String commandWin )
{
    def tasks = [:]

    String oses = "win10,ubuntu"
    oses.split(',').each()
    {
        String os = "${it}"
        tasks["Build-"+os] = executeBuilds(projectBranch, os, commandLinux, commandWin )

    }

    parallel tasks
}

def executeTests(String testPlatforms, 
    String testCmdWinCpu, String testCmdWinGpu, 
    String testCmdLinuxCpu, String testCmdLinuxGpu,
    def deployFunc )
{
    def tasks = [:]

    testPlatforms.split(',').each()
    {
        def (os, gpu) = it.tokenize(':')
        tasks[os+"-"+gpu] = executeTestsImpl( os, gpu, 
            testCmdWinCpu, testCmdWinGpu, testCmdLinuxCpu, testCmdLinuxGpu, 
            deployFunc )        
    }

    parallel tasks
}

def deployImpl()
{
    archiveArtifacts artifacts: 'dist/release/**/*'
    junit 'scripts/*.xml'
}

def call(String projectBranch='', String testPlatforms = "win10:cpu,win10:vega,win10:fiji,ubuntu:fiji", Boolean enableNotifications = true) 
{
    String buildCmdLinux = './scripts/build/macos/buildTahoe.sh'
    String buildCmdWin = './scripts/build/win/buildTahoe.bat'
    
    String testCmdWinCpu = './scripts/test/win/tahoeTestsCpu.bat'
    String testCmdWinGpu = './scripts/test/win/tahoeTestsGpu.bat'
    String testCmdLinuxCpu = './scripts/test/macos/tahoeTestsCpu.sh'
    String testCmdLinuxGpu = './scripts/test/macos/tahoeTestsGpu.sh'
    try 
    {
        timestamps {
            executeBuilds( projectBranch, buildCmdLinux, buildCmdWin )
            executeTests(testPlatforms, testCmdWinCpu, testCmdWinGpu, testCmdLinuxCpu, testCmdLinuxGpu,
                this.&deployImpl )
        }
    }
    finally 
    {
    }
}
