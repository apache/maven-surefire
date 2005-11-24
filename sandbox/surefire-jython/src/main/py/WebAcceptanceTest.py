import WebSuite

class WebAcceptanceTest(WebSuite):
    
    def testWeb(self):
        self.getTestContext().setBaseUrl("http://www.google.com")
        self.beginAt("/");
        self.setFormElement("q", "httpunit");
        self.submit("btnG");
        self.clickLinkWithText("HttpUnit");
        self.assertTitleEquals("HttpUnit");
        self.assertLinkPresentWithText("User's Manual");
