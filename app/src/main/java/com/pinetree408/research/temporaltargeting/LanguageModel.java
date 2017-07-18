package com.pinetree408.research.temporaltargeting;

/**
 * Created by user on 2017-07-18.
 */

import java.util.*;
import java.io.*;

public class LanguageModel {
    public List<Anc> ancList = new ArrayList<Anc>();

    public LanguageModel() {
        ancList = getAncList();
    }

    class Anc {
        public String word;
        public int freq;
        public Anc( String word, int freq) {
            this.word = word;
            this.freq = freq;
        }
    }

    class AlphaFreq {
        public String alpha;
        public int freq;
        public AlphaFreq( String alpha) {
            this.alpha = alpha;
            freq = 0;
        }
    }


    public List<Anc> getAncList() {
        try {
            BufferedReader ancFile = new BufferedReader(new FileReader("ANC-all-count.txt"));

            String line;
            String[] lines;
            while(( line = ancFile.readLine()) != null) {
                lines = line.split("\t");
                String word = lines[0];
                int freq = Integer.parseInt( lines[3]);

                int chk = 1;
                for( Anc anc : ancList) {
                    if( word.equals( anc.word)) {
                        anc.freq += freq;
                        chk = 0;
                        break;
                    }
                }

                if( chk == 1) {
                    Anc anc = new Anc( word, freq);
                    ancList.add( anc);
                }

                if( ancList.size() == 15000) {
                    break;
                }
            }
        } catch( Exception e) {
            System.err.println("IO Error");
        }
        return ancList;
    }
    /* return list of Anc object.
    each Anc has a word and freq of the word.
    the order of list is sorted by freq of the word.
    */
    List<Anc> getAncsFromPrefix( String prefix) {
        List<Anc> ret = new ArrayList<Anc>();
        for( Anc anc : ancList) {
            if( anc.word.startsWith( prefix)) {
                ret.add( anc);
            }
        }
        return ret;
    }

    /* return list of word.
    the order of list is sorted by freq of the word.
    */
    List<String> getWordsFromPrefix( String prefix) {
        List<String> ret = new ArrayList<String>();
        List<Anc> ancPrefixList = getAncsFromPrefix( prefix);
        for( Anc anc : ancPrefixList) {
            ret.add( anc.word);
        }
        return ret;
    }

    /* return lint of AlphaFreq object list.
    each AlphaFreq has single alphabet character and freq of startswith(prefix+alpha).
    the order is not sorted.
    */
    List<AlphaFreq> getAlphaFreqsFromPrefix( String prefix) {
        List<Anc> ancs = getAncsFromPrefix( prefix);
        List<AlphaFreq> alphaFreqs = new ArrayList<AlphaFreq>();

        for( int i = 0; i < 25; i++) {
            AlphaFreq alphaFreq = new AlphaFreq("" + (char)( 'a' + i));
            alphaFreqs.add( alphaFreq);
            for( Anc anc : ancList) {
                if( anc.word.startsWith( prefix + alphaFreq.alpha)) {
                    alphaFreq.freq += anc.freq;
                }
            }
        }
        return alphaFreqs;
    }

    /* return list of alphabet.
    the order of list is sorted by freq of startswith(prefix+alpha).
    */
    List<String> getAlphasFromPrefix( String prefix) {
        List<AlphaFreq> alphaFreqs = getAlphaFreqsFromPrefix( prefix);
        Collections.sort(alphaFreqs, new FreqComparator());
        List<String> alphas = new ArrayList<String>();
        for( AlphaFreq alphaFreq : alphaFreqs) {
            alphas.add( alphaFreq.alpha);
        }
        return alphas;
    }

    class FreqComparator implements Comparator<AlphaFreq> {
        @Override
        public int compare( AlphaFreq a, AlphaFreq b) {
            return (a.freq > b.freq)? -1 : (a.freq == b.freq)? 0 : 1;
        }
    }
    public static void main( String[] args) {
        LanguageModel lm = new LanguageModel();
        System.out.println( lm.getWordsFromPrefix( "the"));
        System.out.println( lm.getAlphasFromPrefix( "th"));
    }
}
