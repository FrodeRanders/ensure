group test;

delimiters "$", "$"

preamble() ::= <<
/*
 * This code was automatically generated. Manual modifications to this source code file will be ignored!
 */
>>

block(package,name,strings,doViewCompileTimeData) ::= <<
package $package$;

import java.util.List;

public class $name$
{
$if (doViewCompileTimeData)$
  $stringArray(...)$
$endif$

  public $name$() {
  }

  public void test(List<String> list) {
$if (doViewCompileTimeData)$
      System.out.print("Code generation time data: ");
      for (String i : stringArray) {
          System.out.print(i);
          System.out.print(", ");
      }
      System.out.println("");
$endif$

      System.out.print("Program call time data: ");
      for (String i : list) {
          System.out.print(i);
          System.out.print(", ");
      }
      System.out.println("\n");
  }
}
>>

stringArray(strings) ::= <<
private String[] stringArray = { "$strings; separator="\", \""$" };
>>
