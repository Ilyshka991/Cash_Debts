package com.pechuro.cashdebts.calculator.exception

abstract class CalculatorException : Exception()

class InvalidExpressionException : CalculatorException()