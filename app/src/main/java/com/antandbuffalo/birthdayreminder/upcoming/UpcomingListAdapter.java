package com.antandbuffalo.birthdayreminder.upcoming;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.antandbuffalo.birthdayreminder.R;
import com.antandbuffalo.birthdayreminder.database.DateOfBirthDBHelper;
import com.antandbuffalo.birthdayreminder.models.DateOfBirth;
import com.antandbuffalo.birthdayreminder.utilities.Constants;
import com.antandbuffalo.birthdayreminder.utilities.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Created by i677567 on 23/9/15.
 */
public class UpcomingListAdapter extends BaseAdapter {

    int currentDayOfYear, dayOfYear, recentDayOfYear;
    Calendar cal;
    List<DateOfBirth> dobs;
    List<DateOfBirth> allDobs;
    SimpleDateFormat dateFormatter;
    List<DateOfBirth> filteredDobs = new ArrayList<DateOfBirth>();
    public UpcomingListAdapter() {
        dateFormatter = new SimpleDateFormat("MMM");
        cal = Calendar.getInstance();
        setDefaultValues();
    }

    @Override
    public int getCount() {
        if(dobs == null) {
            return 0;
        }
        return dobs.size();
    }

    @Override
    public DateOfBirth getItem(int position) {
        return dobs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        DateOfBirth dob = dobs.get(position);
        dayOfYear = Integer.parseInt(Util.getStringFromDate(dob.getDobDate(), Constants.DAY_OF_YEAR));
        return dayOfYear == currentDayOfYear? 0 : 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        DateOfBirth dob = dobs.get(position);
        dayOfYear = Integer.parseInt(Util.getStringFromDate(dob.getDobDate(), Constants.DAY_OF_YEAR));
        if(convertView == null) {
            if(dayOfYear == currentDayOfYear) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item_today, parent, false);
            }
            else {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item_default, parent, false);
            }
        }
        TextView name = (TextView)convertView.findViewById(R.id.nameField);
        TextView desc = (TextView)convertView.findViewById(R.id.ageField);
        TextView yearField = (TextView)convertView.findViewById(R.id.yearField);
        name.setText(dob.getName());
        desc.setText(dob.getDescription());
        Date date = dob.getDobDate();
        cal.setTime(date);
        yearField.setText(cal.get(Calendar.YEAR) + "");

        if(dayOfYear != currentDayOfYear) {
            TextView dateField = (TextView)convertView.findViewById(R.id.dateField);
            TextView monthField = (TextView)convertView.findViewById(R.id.monthField);

            dateField.setText(cal.get(Calendar.DATE) + "");
            monthField.setText(dateFormatter.format(cal.getTime()));

            LinearLayout circle = (LinearLayout)convertView.findViewById(R.id.circlebg);
            dayOfYear = Integer.parseInt(Util.getStringFromDate(dob.getDobDate(), Constants.DAY_OF_YEAR));
            if(dayOfYear == currentDayOfYear) {
                circle.setBackgroundResource(R.drawable.cirlce_today);
            }
            else if(recentDayOfYear < currentDayOfYear) {   //year end case
                if(dayOfYear > currentDayOfYear || dayOfYear < recentDayOfYear) {
                    circle.setBackgroundResource(R.drawable.cirlce_recent);
                }
                else {
                    circle.setBackgroundResource(R.drawable.cirlce_normal);
                }
            }
            else if(dayOfYear <= recentDayOfYear && dayOfYear > currentDayOfYear ){
                circle.setBackgroundResource(R.drawable.cirlce_recent);
            }
            else {
                circle.setBackgroundResource(R.drawable.cirlce_normal);
            }
        }
        if(dob.getRemoveYear()) {
            yearField.setVisibility(View.INVISIBLE);
            desc.setVisibility(View.VISIBLE);
        }
        else {
            if(dob.getAge() >= 0) {
                desc.setVisibility(View.VISIBLE);
            }
            else {
                desc.setVisibility(View.INVISIBLE);
            }
            yearField.setVisibility(View.VISIBLE);
        }
        return convertView;
    }

    public void refreshData() {
        setDefaultValues();
        this.notifyDataSetChanged();
    }

    public void setDefaultValues() {
        currentDayOfYear = Integer.parseInt(Util.getStringFromDate(new Date(), Constants.DAY_OF_YEAR));
        cal.setTime(new Date());
        cal.add(Calendar.DATE, Constants.RECENT_DURATION);
        recentDayOfYear = Integer.parseInt(Util.getStringFromDate(cal.getTime(), Constants.DAY_OF_YEAR));
        allDobs = DateOfBirthDBHelper.selectUpcoming();
        dobs = DateOfBirthDBHelper.selectUpcoming();
    }

    public void filter(String input) {
        filteredDobs.clear();
        // perform your search here using the searchConstraint String.
        DateOfBirth dob;
        String dateString, monthString;
        for (int i = 0; i < allDobs.size(); i++) {
            dob = allDobs.get(i);
            dateString = Util.getStringFromDate(dob.getDobDate());
            monthString = dateFormatter.format(Util.getCalendar(dob.getDobDate()).getTime());
            input = input.toLowerCase();
            if (dob.getName().toLowerCase().contains(input) || dateString.contains(input) || monthString.toLowerCase().contains(input))  {
                filteredDobs.add(dob);
            }
        }
        dobs = filteredDobs;
    }
}
