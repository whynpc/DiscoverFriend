package edu.ucla.discoverfriend;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.hash.BloomFilter;

public class CustomNetworkPacket implements Parcelable {
	private BloomFilter<String> bf;	
	private BloomFilter<String> bfc;
	private String cf = "certificate";
	
	public CustomNetworkPacket(BloomFilter<String> bf, BloomFilter<String> bfc, String cf) {
		this.bf = bf;
		this.bfc = bfc;
		this.cf = cf;
	}
	
	public CustomNetworkPacket(Parcel parcel) {
		ClassLoader cl = BloomFilter.class.getClassLoader();
		bf = (BloomFilter<String>) parcel.readValue(cl);
		bfc = (BloomFilter<String>) parcel.readValue(cl);
		cf = parcel.readString();
	}

	public BloomFilter<String> getBf() {
		return bf;
	}

	public void setBf(BloomFilter<String> bf) {
		this.bf = bf;
	}

	public BloomFilter<String> getBfc() {
		return bfc;
	}

	public void setBfc(BloomFilter<String> bfc) {
		this.bfc = bfc;
	}
	
	public String getCf() {
		return cf;
	}

	public void setCf(String cf) {
		this.cf = cf;
	}

	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(bf);
		dest.writeValue(bfc);
		dest.writeString(cf);
	}
	
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public CustomNetworkPacket createFromParcel(Parcel in) {
            return new CustomNetworkPacket(in); 
        }

        public CustomNetworkPacket[] newArray(int size) {
            return new CustomNetworkPacket[size];
        }
    };
	
}
