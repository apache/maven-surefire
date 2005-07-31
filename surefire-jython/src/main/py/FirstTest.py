from org.suiterunner import Suite

class FirstTest(Suite):
    
    def __init__(self):
        self.name = "jason"
        
    def testBar(self):
        self.verify( self.name == "jason" )

    def testFoo(self):
        self.verify( 0 == 0 )
