package com.revnatproxy;

import java.util.HashMap;
import java.util.ArrayList;
import java.lang.System;
import java.io.UnsupportedEncodingException;

public class Bin{
    static public final int BLOCK_SIZE = 4096;
    public int lastLength_;
    public ArrayList<byte[]> binData_;
    
    public Bin(){
        lastLength_ = 0;
        binData_ = new ArrayList<byte[]>();
    }
    
    public void addBinData(byte[] src, int offset, int size){
        for(int i = 0; i < size; ){
            if(binData_.size() == 0 || lastLength_ >= BLOCK_SIZE){
                //Insert new block
                binData_.add(new byte[BLOCK_SIZE]);
                lastLength_ = 0;
            }
            int copySize = BLOCK_SIZE - lastLength_;
            if(copySize > size - i) copySize = size - i;
            
            System.arraycopy(src, i, binData_.get(binData_.size() - 1), lastLength_, copySize);
            lastLength_ += copySize;
            i += copySize;
        }
    }
    public void addBinData(Bin bin){
        for(int i = 0; i < bin.binData_.size(); ++i){
            int size = bin.getBlockSize(i);
            addBinData(bin.binData_.get(i), 0, size);
        }
    }
    public void addStrData(String s){
        try{
            if(s == null) return;
            byte[] src = s.getBytes("UTF-8");
            addBinData(src, 0, src.length);
        }catch(UnsupportedEncodingException ex){
        }
    }
    
    public void setStrData(String s){
        binData_.clear();
        addStrData(s);
    }
    
    public String toString(){
        String s = "";
        for(int i = 0; i < getBlockCount(); ++i){
            s += new String(binData_.get(i), 0, getBlockSize(i));
        }
        return s;
    }
    
    public int getBlockSize(int pos){
        if(pos + 1 >= binData_.size()) return lastLength_;
        else return BLOCK_SIZE;
    }
    
    public int getBlockCount(){
        return binData_.size();
    }
    
    public byte[] getBlock(int pos){
        return binData_.get(pos);
    }
    
    public int size(){
        if(binData_.size() == 0)
            return 0;
        else
            return binData_.size() * BLOCK_SIZE + lastLength_ - BLOCK_SIZE;
    }
}
