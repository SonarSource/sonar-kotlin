x = "pass";
"pass";
x = "password";
"password";
x = "login=a&password=";
"login=a&password=";
"login=a&password= " + value;
"login=a&password=a"; // Noncompliant
x = "login=a&password=xxx"; // Noncompliant {{"password" detected here, make sure this is not a hard-coded credential.}}
//  ^^^^^^^^^^^^^^^^^^^^^^
"login=a&password=xxx"; // Noncompliant
"login=a&passwd=xxx"; // Noncompliant {{"passwd" detected here, make sure this is not a hard-coded credential.}}
"login=a&pwd=xxx"; // Noncompliant {{"pwd" detected here, make sure this is not a hard-coded credential.}}
"login=a&passphrase=xxx"; // Noncompliant {{"passphrase" detected here, make sure this is not a hard-coded credential.}}
  variableNameWithPasswordInIt = "xxx"; // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^
variableNameWithPasswdInIt = "xxx"; // Noncompliant
variableNameWithPasswdInIt += "xxx"; // Noncompliant
variableNameWithPwdInIt = "xxx";  // Noncompliant {{"Pwd" detected here, make sure this is not a hard-coded credential.}}
A.variableNameWithPwdInIt = "xxx";  // Noncompliant {{"Pwd" detected here, make sure this is not a hard-coded credential.}}
A.B.variableNameWithPwdInIt = "xxx";  // Noncompliant {{"Pwd" detected here, make sure this is not a hard-coded credential.}}
otherVariableNameWithPasswordInIt;
variableNameWithPasswordInIt = native[] { [ "NativeFunctionCall" ] };
val constValue = "login=a&password=xxx"; // Noncompliant
var passwd = "xxxx"; // Noncompliant
var passphrase = "xxx"; // Noncompliant
var okVariable = "xxxx";
variableNameWithPasswdInIt = "";
A.B.variableNameWithPwdInIt = "";
var passwd = "";
var passwd = 2;

// No issue is raised when the matched wordlist item is present in both symbol name and literal string value.
var password = "password"; // Compliant
var myPassword = "users/connection.secretPassword"; // Compliant
myPassword = "users/connection.secretPassword"; // Compliant
var myPassword = "secretPasswd"; // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
myPassword = "secretPasswd"; // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
var params = "user=admin&password=Password123"; // Noncompliant {{"password" detected here, make sure this is not a hard-coded credential.}}

// Database queries are compliant
var query = "password=?";
query = "password=:password";
query = "password=:param";
query = "password='" + password + "'";
query = "password=%s";
query = "password=%v";

// String format is compliant
query = "password={0}";

// Support URI
var uri = "http://user:azer:ty123@domain.com"; // Noncompliant
var uri = "https://:azerty123@domain.com/path"; // Noncompliant {{Review this hard-coded URL, which may contain a credential.}}
var uri = "http://anonymous:anonymous@domain.com"; // Compliant, user and password are the same
var uri = "http://user:@domain.com";
var uri = "http://user@domain.com:80";
var uri = "http://domain.com/user:azerty123";
var uri = "too-long-url-scheme://user:123456@server.com";
var uri = "https:// invalid::url::format";
