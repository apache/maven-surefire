from org.codehaus.plexus.surefire.web.functional import FunctionalWebSuite

class MyFunctionalWebSuite(FunctionalWebSuite):
    
    def testGoogleResponse(self):
        self.get( "http://www.google.com" )
        self.responseOK()
