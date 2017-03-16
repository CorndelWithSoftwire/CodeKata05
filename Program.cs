using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;

namespace CodeKata05
{
  class Program
  {
    private static readonly Random Random = new Random();

    static void Main()
    {
      var wordList = new HashSet<string>(new WebClient().DownloadString(@"http://codekata.com/data/wordlist.txt").Split('\n'));

      Console.WriteLine("HashLength | Hash Count | Density                 | False +ves | Valid Words      ");

      for (int hashLength = 16; hashLength < 31; hashLength += 2)
      {
        for (int hashCount = 1; hashCount < 256 / (8 * Math.Ceiling(hashLength / 8.0)); hashCount += 2)
        {
          var filter = CreateBloomFilter(hashLength, hashCount, wordList);
          var density = filter.Count(bit => bit);
          (var validWords, var falsePositives, var totalWords) = TestBloomFilter(hashLength, hashCount, filter, wordList);

          Console.WriteLine($"{hashLength,10} | {hashCount,10} | {density,10} / {filter.Length,10} | {falsePositives,10} | {validWords,10} / {totalWords,10}");
        }
      }

      Console.ReadLine();
    }

    private static bool[] CreateBloomFilter(int hashLength, int hashCount, ICollection<string> wordList)
    {
      var bitmap = new bool[(long)Math.Pow(2, hashLength)];

      foreach (var word in wordList)
      {
        foreach (var hash in GetHashes(word, hashLength).Take(hashCount))
        {
          bitmap[hash] = true;
        }
      }
      return bitmap;
    }

    private static (int, int, int) TestBloomFilter(int hashLength, int hashCount, bool[] bitmap, ISet<string> wordList)
    {
      int validWords = 0;
      int falsePositives = 0;
      const int totalWords = 5000;

      foreach (var possibleWord in RandomWords.Take(totalWords))
      {
        var match = true;

        foreach (var hash in GetHashes(possibleWord, hashLength).Take(hashCount))
        {
          if (!bitmap[hash])
          {
            match = false;
            break;
          }
        }

        if (match)
        {
          if (wordList.Contains(possibleWord))
          {
            validWords++;
          }
          else
          {
            falsePositives++;
          }
        }
        else if (wordList.Contains(possibleWord))
        {
          throw new InvalidOperationException("Bloom Filter missed a match!");
        }
      }
      return (validWords, falsePositives, totalWords);
    }

    private static IEnumerable<long> GetHashes(string word, int hashLength)
    {
      var md5Hash = new MD5CryptoServiceProvider().ComputeHash(Encoding.ASCII.GetBytes(word));

      var workingHash = 0L;
      var workingLength = 0;

      foreach (var hashByte in md5Hash)
      {
        workingHash = workingHash << 8;
        workingHash = workingHash | hashByte;
        workingLength += 8;

        if (workingLength >= hashLength)
        {
          workingHash = workingHash >> (workingLength - hashLength);

          yield return workingHash;
          workingHash = 0;
          workingLength = 0;
        }
      }
    }

    public static IEnumerable<string> RandomWords
    {
      get
      {
        while (true)
        {
          yield return string.Join("", RandomLetters.Take(5));
        }
      }
    }

    public static IEnumerable<char> RandomLetters
    {
      get
      {
        while (true)
        {
          // Only pick lower case letters for now
          var charCode = 'a' + Random.Next(0, 25);
          yield return (char)charCode;
        }
      }
    }
  }
}
