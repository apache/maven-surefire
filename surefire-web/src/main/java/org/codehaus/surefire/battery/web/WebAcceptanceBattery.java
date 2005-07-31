package org.codehaus.surefire.battery.web;

/*
 * Copyright 2001-2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.PrintStream;
import net.sourceforge.jwebunit.*;
import org.codehaus.surefire.battery.AbstractBattery;

public class WebAcceptanceBattery
    extends AbstractBattery
{

    public WebAcceptanceBattery()
    {
        tester = new WebTester();
    }

    public HttpUnitDialog getDialog()
    {
        return tester.getDialog();
    }

    public TestContext getTestContext()
    {
        return tester.getTestContext();
    }

    public void beginAt(String relativeURL)
    {
        tester.beginAt(relativeURL);
    }

    public String getMessage(String key)
    {
        return tester.getMessage(key);
    }

    public void assertTitleEquals(String title)
    {
        tester.assertTitleEquals(title);
    }

    public void assertTitleEqualsKey(String titleKey)
    {
        tester.assertTitleEqualsKey(titleKey);
    }

    public void assertKeyPresent(String key)
    {
        tester.assertKeyPresent(key);
    }

    public void assertTextPresent(String text)
    {
        tester.assertTextPresent(text);
    }

    public void assertKeyNotPresent(String key)
    {
        tester.assertKeyNotPresent(key);
    }

    public void assertTextNotPresent(String text)
    {
        tester.assertTextNotPresent(text);
    }

    public void assertTablePresent(String tableSummaryOrId)
    {
        tester.assertTablePresent(tableSummaryOrId);
    }

    public void assertTableNotPresent(String tableSummaryOrId)
    {
        tester.assertTableNotPresent(tableSummaryOrId);
    }

    public void assertKeyInTable(String tableSummaryOrId, String key)
    {
        tester.assertKeyInTable(tableSummaryOrId, key);
    }

    public void assertTextInTable(String tableSummaryOrId, String text)
    {
        tester.assertTextInTable(tableSummaryOrId, text);
    }

    public void assertKeysInTable(String tableSummaryOrId, String keys[])
    {
        tester.assertKeysInTable(tableSummaryOrId, keys);
    }

    public void assertTextInTable(String tableSummaryOrId, String text[])
    {
        tester.assertTextInTable(tableSummaryOrId, text);
    }

    public void assertKeyNotInTable(String tableSummaryOrId, String key)
    {
        tester.assertKeyNotInTable(tableSummaryOrId, key);
    }

    public void assertTextNotInTable(String tableSummaryOrId, String text)
    {
        tester.assertTextNotInTable(tableSummaryOrId, text);
    }

    public void assertTextNotInTable(String tableSummaryOrId, String text[])
    {
        tester.assertTextNotInTable(tableSummaryOrId, text);
    }

    public void assertTableEquals(String tableSummaryOrId, ExpectedTable expectedTable)
    {
        tester.assertTableEquals(tableSummaryOrId, expectedTable.getExpectedStrings());
    }

    public void assertTableEquals(String tableSummaryOrId, String expectedCellValues[][])
    {
        tester.assertTableEquals(tableSummaryOrId, expectedCellValues);
    }

    public void assertTableRowsEqual(String tableSummaryOrId, int startRow, ExpectedTable expectedTable)
    {
        tester.assertTableRowsEqual(tableSummaryOrId, startRow, expectedTable);
    }

    public void assertTableRowsEqual(String tableSummaryOrId, int startRow, String expectedCellValues[][])
    {
        tester.assertTableRowsEqual(tableSummaryOrId, startRow, expectedCellValues);
    }

    public void assertFormElementPresent(String formElementName)
    {
        tester.assertFormElementPresent(formElementName);
    }

    public void assertFormElementNotPresent(String formElementName)
    {
        tester.assertFormElementNotPresent(formElementName);
    }

    public void assertFormElementPresentWithLabel(String formElementLabel)
    {
        tester.assertFormElementPresentWithLabel(formElementLabel);
    }

    public void assertFormElementNotPresentWithLabel(String formElementLabel)
    {
        tester.assertFormElementNotPresentWithLabel(formElementLabel);
    }

    public void assertFormPresent()
    {
        tester.assertFormPresent();
    }

    public void assertFormPresent(String formName)
    {
        tester.assertFormPresent(formName);
    }

    public void assertFormElementEquals(String formElementName, String expectedValue)
    {
        tester.assertFormElementEquals(formElementName, expectedValue);
    }

    public void assertFormElementEmpty(String formElementName)
    {
        tester.assertFormElementEmpty(formElementName);
    }

    public void assertCheckboxSelected(String checkBoxName)
    {
        tester.assertCheckboxSelected(checkBoxName);
    }

    public void assertCheckboxNotSelected(String checkBoxName)
    {
        tester.assertCheckboxNotSelected(checkBoxName);
    }

    public void assertRadioOptionPresent(String radioGroup, String radioOption)
    {
        tester.assertRadioOptionPresent(radioGroup, radioOption);
    }

    public void assertRadioOptionNotPresent(String radioGroup, String radioOption)
    {
        tester.assertRadioOptionNotPresent(radioGroup, radioOption);
    }

    public void assertRadioOptionSelected(String radioGroup, String radioOption)
    {
        tester.assertRadioOptionSelected(radioGroup, radioOption);
    }

    public void assertRadioOptionNotSelected(String radioGroup, String radioOption)
    {
        tester.assertRadioOptionNotSelected(radioGroup, radioOption);
    }

    public void assertOptionsEqual(String selectName, String options[])
    {
        tester.assertOptionsEqual(selectName, options);
    }

    public void assertOptionsNotEqual(String selectName, String options[])
    {
        tester.assertOptionsNotEqual(selectName, options);
    }

    public void assertOptionValuesEqual(String selectName, String options[])
    {
        tester.assertOptionValuesEqual(selectName, options);
    }

    public void assertOptionValuesNotEqual(String selectName, String options[])
    {
        tester.assertOptionValuesNotEqual(selectName, options);
    }

    public void assertOptionEquals(String selectName, String option)
    {
        tester.assertOptionEquals(selectName, option);
    }

    public void assertSubmitButtonPresent(String buttonName)
    {
        tester.assertSubmitButtonPresent(buttonName);
    }

    public void assertSubmitButtonNotPresent(String buttonName)
    {
        tester.assertSubmitButtonNotPresent(buttonName);
    }

    public void assertSubmitButtonValue(String buttonName, String expectedValue)
    {
        tester.assertSubmitButtonValue(buttonName, expectedValue);
    }

    public void assertButtonPresent(String buttonID)
    {
        tester.assertButtonPresent(buttonID);
    }

    public void assertButtonNotPresent(String buttonID)
    {
        tester.assertButtonNotPresent(buttonID);
    }

    public void assertLinkPresent(String linkId)
    {
        tester.assertLinkPresent(linkId);
    }

    public void assertLinkNotPresent(String linkId)
    {
        tester.assertLinkNotPresent(linkId);
    }

    public void assertLinkPresentWithText(String linkText)
    {
        tester.assertLinkPresentWithText(linkText);
    }

    public void assertLinkNotPresentWithText(String linkText)
    {
        tester.assertLinkNotPresentWithText(linkText);
    }

    public void assertLinkPresentWithImage(String imageFileName)
    {
        tester.assertLinkPresentWithImage(imageFileName);
    }

    public void assertLinkNotPresentWithImage(String imageFileName)
    {
        tester.assertLinkNotPresentWithImage(imageFileName);
    }

    public void assertElementPresent(String anID)
    {
        tester.assertElementPresent(anID);
    }

    public void assertElementNotPresent(String anID)
    {
        tester.assertElementNotPresent(anID);
    }

    public void assertTextInElement(String elID, String text)
    {
        tester.assertTextInElement(elID, text);
    }

    public void assertWindowPresent(String windowName)
    {
        tester.assertWindowPresent(windowName);
    }

    public void assertFramePresent(String frameName)
    {
        tester.assertFramePresent(frameName);
    }

    public void setWorkingForm(String nameOrId)
    {
        tester.setWorkingForm(nameOrId);
    }

    protected void setFormElement(String formElementName, String value)
    {
        tester.setFormElement(formElementName, value);
    }

    protected void checkCheckbox(String checkBoxName)
    {
        tester.checkCheckbox(checkBoxName);
    }

    protected void uncheckCheckbox(String checkBoxName)
    {
        tester.uncheckCheckbox(checkBoxName);
    }

    public void selectOption(String selectName, String option)
    {
        tester.selectOption(selectName, option);
    }

    protected void submit()
    {
        tester.submit();
    }

    public void submit(String buttonName)
    {
        tester.submit(buttonName);
    }

    public void reset()
    {
        tester.reset();
    }

    protected void clickLinkWithText(String linkText)
    {
        tester.clickLinkWithText(linkText);
    }

    protected void clickLinkWithImage(String imageFileName)
    {
        tester.clickLinkWithImage(imageFileName);
    }

    protected void clickLink(String linkId)
    {
        tester.clickLink(linkId);
    }

    protected void clickButton(String buttonId)
    {
        tester.clickButton(buttonId);
    }

    public void gotoRootWindow()
    {
        tester.gotoRootWindow();
    }

    public void gotoWindow(String windowName)
    {
        tester.gotoWindow(windowName);
    }

    public void gotoFrame(String frameName)
    {
        tester.gotoFrame(frameName);
    }

    protected void dumpResponse(PrintStream stream)
    {
        tester.dumpResponse(stream);
    }

    protected void dumpTable(String tableNameOrId, PrintStream stream)
    {
        tester.dumpTable(tableNameOrId, stream);
    }

    protected void dumpTable(String tableNameOrId, String table[][])
    {
        tester.dumpTable(tableNameOrId, table);
    }

    private WebTester tester;
}
