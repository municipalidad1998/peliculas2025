#!/usr/bin/env python3
"""
Tests for the greeting module
"""

import unittest
from greeting import greet


class TestGreeting(unittest.TestCase):
    """Test cases for greeting functionality"""

    def test_greet_returns_string(self):
        """Test that greet() returns a string"""
        result = greet()
        self.assertIsInstance(result, str)

    def test_greet_contains_hola(self):
        """Test that the greeting contains 'Hola'"""
        result = greet()
        self.assertIn("Hola", result)

    def test_greet_not_empty(self):
        """Test that the greeting is not empty"""
        result = greet()
        self.assertGreater(len(result), 0)


if __name__ == "__main__":
    unittest.main()
