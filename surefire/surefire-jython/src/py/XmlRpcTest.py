import XmlRpcSuite

class XmlRpcTest(XmlRpcSuite):
    
    def testXmlRpc(self):
        self.setupServer("http://time.xmlrpc.com/RPC2")
        response = self.send( "currentTime.getCurrentTime" )
        self.verify( response != None )
        
        print "the current time is ", response
