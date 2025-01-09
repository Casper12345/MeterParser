package parser

import munit.FunSuite
import domain.NEM12Record
import java.time.LocalDate
import util.FileUtil

class NEM12ParserSuite extends FunSuite {

  test("parseLine should parse a valid Header record") {
    val line = "100,NEM12,200506081149,UNITEDDP,NEMMCO"
    val result = NEM12Parser.parseLine(line)

    assertEquals(result, Right(NEM12Record.Header()))
  }

  test("parseLine should fail for Header with non-NEM12 format") {
    val line = "100,NEM13,200506081149,UNITEDDP,NEMMCO"
    val result = NEM12Parser.parseLine(line)

    assert(result.isLeft)
    assert(result.swap.exists(_.isInstanceOf[InvalidFormatException]))
    assertEquals(result.swap.toOption.get.getMessage, "Parser only supports NEM12 format")
  }

  test("parseLine should parse a valid NMIDataDetails record") {
    val line = "200,NEM1201015,E1E2,1,E1,N1,01009,kWh,30,20050610"
    val result = NEM12Parser.parseLine(line)

    assertEquals(
      result,
      Right(NEM12Record.NMIDataDetails(nmi = "NEM1201015", intervalLength = 30))
    )
  }

  test("parseLine should fail for NMIDataDetails with invalid interval length") {
    val line = "200,NEM1201015,E1E2,1,E1,N1,01009,kWh,33,20050610"
    val result = NEM12Parser.parseLine(line)

    assert(result.isLeft)
    assert(result.swap.exists(_.isInstanceOf[InvalidFormatException]))
    assertEquals(result.swap.toOption.get.getMessage, "Invalid NEM12 interval length")
  }

  test("parseLine should parse a valid IntervalData record") {
    val lines = FileUtil.getLines("csv/testLines.csv")
    val line = lines.head

    val result = NEM12Parser.parseLine(line)

    assertEquals(
      result,
      Right(NEM12Record.IntervalData(
        intervalDate = LocalDate.of(2005, 3, 1),
        intervalValues = List(
          0,0,0,0,0,0,0,0,0,0,0,0,
          0.461,0.810,0.568,1.234,
          1.353,1.507,1.344,1.773,0.848,
          1.271,0.895,1.327,1.013,1.793,
          0.988,0.985,0.876,0.555,0.760,
          0.938,0.566,0.512,0.970,0.760,
          0.731,0.615,0.886,0.531,0.774
          ,0.712,0.598,0.670,0.587,0.657
          ,0.345,0.231
        )
      ))
    )
  }

  test("parseLine should fail for IntervalData with invalid date") {
    val lines = FileUtil.getLines("csv/testLines.csv")
    val line = lines(1)

    val result = NEM12Parser.parseLine(line)

    assert(result.isLeft)
    assert(result.swap.exists(_.isInstanceOf[InvalidDataException]))
    assertEquals(
      result.swap.toOption.get.getMessage,
      "Failed to parse date Text '20230132' could not be parsed: Invalid value for DayOfMonth (valid values 1 - 28/31): 32"
    )
  }

  test("parseLine should fail if interval data cannot be parsed to double") {
    val lines = FileUtil.getLines("csv/testLines.csv")
    val line = lines(2)

    val result = NEM12Parser.parseLine(line)

    assert(result.isLeft)
    assert(result.swap.exists(_.isInstanceOf[InvalidDataException]))
    assertEquals(
      result.swap.toOption.get.getMessage,
      """Failed to parse double For input string: "not-a-double""""
    )
  }

  test("parseLine should parse a valid Footer record") {
    val line = "900"
    val result = NEM12Parser.parseLine(line)

    assertEquals(result, Right(NEM12Record.Footer()))
  }

  test("parseLine should fail for an empty line") {
    val line = ""
    val result = NEM12Parser.parseLine(line)

    assert(result.isLeft)
    assert(result.swap.exists(_.isInstanceOf[InvalidDataException]))
    assertEquals(result.swap.toOption.get.getMessage, """Failed to parse integer For input string: """"")
  }

  test("parseLine should parse 400 to valid ParsableRecord") {
    val line = "400"
    val result = NEM12Parser.parseLine(line)

    assertEquals(result, Right(NEM12Record.ParsableRecord(400)))
  }

  test("parseLine should parse 500 to valid ParsableRecord") {
    val line = "500"
    val result = NEM12Parser.parseLine(line)

    assertEquals(result, Right(NEM12Record.ParsableRecord(500)))
  }

  test("parseLine should fail for an invalid record indicator") {
    val line = "999,SomeData"
    val result = NEM12Parser.parseLine(line)

    assert(result.isLeft)
    assert(result.swap.exists(_.isInstanceOf[InvalidFormatException]))
    assertEquals(result.swap.toOption.get.getMessage, "Invalid NEM12 record indicator 999")
  }
}
