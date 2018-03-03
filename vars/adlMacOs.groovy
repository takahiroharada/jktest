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


def executeBuilds(String oses, String commandLinux,
    def checkoutFunc, def postbuildFunc )
{
    def tasks = [:]

    oses.split(',').each()
    {
        String os = "${it}"
        tasks["Build-"+os] = executeBuildsImpl(os, commandLinux, "", checkoutFunc, postbuildFunc )

    }

    parallel tasks
}

def checkoutImpl()
{
    checkOutBranchOrScm("", 'https://github.com/amdadvtech/adl.git')
}

def postBuildImpl( String os )
{
 
}

def call(String testOses = "macos") 
{
    try 
    {
        String buildCmdMacOs = './scripts/macos/buildAndRunMetal.sh'
        timestamps 
        {
            executeBuilds( testOses, buildCmdMacOs,
                this.&checkoutImpl, this.&postBuildImpl )
        }
    }
    finally 
    {
    }
}
