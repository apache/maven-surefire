from org.suiterunner import Suite

class SecondTest(Suite):
    
    def __init__(self):
        self.name = "bob"
        
    def testDog(self):
        self.verify( self.name == "bob" )

    def testCat(self):
        self.verify( 1 == 1 )
