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
    String artifactPath)
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
                        bat testCommandLinuxGpu
                    else
                        bat testCommandGpu
                }
            }
            stage("Artifact-"+gpu)
            {
                archiveArtifacts artifacts: artifactPath
                junit 'scripts/*.xml'
            }
        }
    }
    return retNode
}

def executeBuilds(String projectBranch, String commandLinux, String commandWin)
{
    def tasks = [:]

    tasks["winBuild"] = executeBuilds(projectBranch, "win10", commandLinux, commandWin )
    tasks["ubuntuBuild"] = executeBuilds(projectBranch, "ubuntu", commandLinux, commandWin )

    parallel tasks
}

def executeTests()
{
    def tasks = [:]

//	String gpus = 'cpu,vega,fiji,quadrok5000,geforce1080'
    String gpus = "cpu,vega,fiji"
    gpus.split(',').each()
    {
        gpu = "${it}"
        println gpu
//        tasks[gpu] = executeTestsImpl( "win10", gpu, 
//            './scripts/test/win/tahoeTestsCpu.bat', './scripts/test/win/tahoeTestsGpu.bat',
//            './scripts/test/macos/tahoeTestsCpu.sh', './scripts/test/macos/tahoeTestsGpu.sh',
//            'dist/release/**/*' )        
    }

    {
//        tasks["ubuntu-fiji"] = executeTests( "ubuntu", "fiji", 
//            './scripts/test/win/tahoeTestsCpu.bat', './scripts/test/win/tahoeTestsGpu.bat',
//            './scripts/test/macos/tahoeTestsCpu.sh', './scripts/test/macos/tahoeTestsGpu.sh',
//            'dist/release/**/*' ) 
    }     

    parallel tasks
}

def call(String projectBranch='', String testPlatforms = 'AMD_RXVEGA;AMD_WX9100;AMD_WX7100', Boolean enableNotifications = true) 
{
    String buildCommandLinux = './scripts/build/macos/buildTahoe.sh'
    String buildCommandWin = './scripts/build/win/buildTahoe.bat'
    try 
    {
        timestamps {
//            executeBuilds( projectBranch, buildCommandLinux, buildCommandWin )
            executeTests()
        }
    }
    finally 
    {
    }
}
